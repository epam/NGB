import {
    StatisticsContainer,
    VariantContainer,
    VCFCollapsedRenderer,
    VCFExpandedRenderer,
    VcfTransformer
} from './internal';
import {GENETrack} from '../gene';
import VcfConfig from './vcfConfig';
import {VcfDataService} from '../../../../dataServices';
import {default as menu} from './menu';
import Menu from '../../core/menu';
import {variantsView} from './modes';
import {menu as menuUtilities, PlaceholderRenderer} from '../../utilities';
import {EventVariationInfo} from '../../../../app/shared/utils/events';

export class VCFTrack extends GENETrack {

    _collapsedRenderer: VCFCollapsedRenderer = null;
    _lastHovered = null;
    _variantsMaximumRange;
    _zoomInRenderer: PlaceholderRenderer = new PlaceholderRenderer(this);

    projectContext;

    static getTrackDefaultConfig() {
        return VcfConfig;
    }

    get stateKeys() {
        return ['variantsView', 'header'];
    }

    get featuresFilteringEnabled () {
        return false;
    }

    static preStateMutatorFn = (track) => ({
        oldVariantsView: track.state.variantsView
    });

    static postStateMutatorFn = (track, key, prePayload) => {
        const {oldVariantsView} = prePayload || {};
        track.transformer.collapsed = track.state.variantsView === variantsView.variantsViewCollapsed;
        if (oldVariantsView !== track.state.variantsView) {
            track.cache = {};
            track._flags.renderReset = true;
        }
        track.updateAndRefresh();
        track.reportTrackState();
    };

    static Menu = Menu(
        menu,
        {
            postStateMutatorFn: VCFTrack.postStateMutatorFn,
            preStateMutatorFn: VCFTrack.preStateMutatorFn
        }
    );

    constructor(opts) {
        super(opts);
        this._variantsMaximumRange = opts.variantsMaximumRange;
        this.transformer.collapsed = this.state.variantsView === variantsView.variantsViewCollapsed;

        this._actions = [
            {
                enabled: function () {
                    return true;
                },
                label: 'Navigation',
                type: 'groupLinks',
                links: [{
                    label: 'Prev',
                    handleClick: ::this.prevVariation
                }, {
                    label: 'Next',
                    handleClick: ::this.nextVariation
                }]
            }
        ];
    }

    async updateCache() {
        if (this._variantsMaximumRange >= this.viewport.actualBrushSize) {
            return await super.updateCache();
        }
        return false;
    }

    trackSettingsChanged(params) {
        if (this.config.bioDataItemId === params.id && this.config.format.toLowerCase() === 'vcf') {
            const settings = params.settings;
            settings.forEach(setting => {
                const menuItem = menuUtilities.findMenuItem(this._menu, setting.name);
                if (menuItem.type === 'checkbox') {
                    menuItem.enable();
                }
            });
        }
    }

    globalSettingsChanged(state) {
        const changed = this._variantsMaximumRange !== state.variantsMaximumRange;
        this._variantsMaximumRange = state.variantsMaximumRange;
        super.globalSettingsChanged(state);
        Promise.resolve().then(async () => {
            if (changed && this._variantsMaximumRange > this.viewport.actualBrushSize) {
                try {
                    this.unsetError();
                    await this.updateCache();
                } catch (e) {
                    this.reportError(e.message);
                }
            }
            if (changed) {
                this._flags.renderReset = true;
            }
            this._flags.renderFeaturesChanged = true;
            this.requestRenderRefresh();
        });
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.constructor.Menu.attach(this, {browserId: this.browserId});
        this.hotKeyListener = (event) => {
            if (event) {
                const path = event.split('>');
                if (path && path[0] === 'vcf') {
                    const menuItem = menuUtilities.findMenuItem(this._menu, event);
                    if (menuItem) {
                        if (menuItem.type === 'button') {
                            menuItem.perform();
                        } else if (menuItem.type === 'checkbox') {
                            menuItem.isEnabled() ? menuItem.disable() : menuItem.enable();
                        }

                    }
                }
            }
        };
        const _hotKeyListener = ::this.hotKeyListener;
        const self = this;
        this._removeHotKeyListener = function () {
            self.dispatcher.removeListener('hotkeyPressed', _hotKeyListener);
        };
        this.dispatcher.on('hotkeyPressed', _hotKeyListener);

        return this._menu;
    }

    get transformer() {
        if (!this._transformer) {
            this._transformer = new VcfTransformer(this.trackConfig, this.config.chromosome);
        }
        return this._transformer;
    }

    get dataService() {
        if (!this._dataService) {
            this._dataService = new VcfDataService(this.dispatcher);
        }
        return this._dataService;
    }

    get downloadHistogramFn() {
        return () => new Promise(resolve => resolve([]));
    }

    get downloadDataFn() {
        return ::this.dataService.loadVcfTrack;
    }

    applyAdditionalRequestParameters(params) {
        params.collapsed = this.state.variantsView === variantsView.variantsViewCollapsed;
    }

    get renderer() {
        if (!this._renderer) {
            this._renderer = new VCFExpandedRenderer(
                this.trackConfig,
                this.transformer,
                this
            );
        }
        if (!this._collapsedRenderer) {
            this._collapsedRenderer = new VCFCollapsedRenderer(
                VcfConfig,
                this
            );
        }
        if (this.state.variantsView === variantsView.variantsViewCollapsed) {
            return this._collapsedRenderer;
        }
        return this._renderer;
    }

    canScroll(delta) {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            return super.canScroll(delta);
        }
        return false;
    }

    isScrollable() {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            return super.isScrollable();
        }
        return false;
    }

    _getZoomInPlaceholderText() {
        const unitThreshold = 1000;
        const noReadText = {
            unit: this._variantsMaximumRange < unitThreshold ? 'BP' : 'kBP',
            value: this._variantsMaximumRange < unitThreshold
                ? this._variantsMaximumRange : Math.ceil(this._variantsMaximumRange / unitThreshold)
        };
        return `Zoom in to see variants.
Minimal zoom level is at ${noReadText.value}${noReadText.unit}`;
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this._zoomInRenderer.container);
            this._zoomInRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        } else if (flags.widthChanged || flags.heightChanged) {
            this._zoomInRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        }
        this._zoomInRenderer.container.visible = this._variantsMaximumRange < this.viewport.actualBrushSize;
        this.renderer.container.visible = this._variantsMaximumRange >= this.viewport.actualBrushSize;
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            return super.render(flags);
        } else {
            let somethingChanged = false;
            if (flags.renderReset) {
                this.container.addChild(this.renderer.container);
                somethingChanged = true;
            }
            if (flags.brushChanged || flags.widthChanged || flags.heightChanged || flags.renderReset || flags.dataChanged) {
                this.renderer.height = this.height;
                this.renderer.render(this.viewport, this.cache, flags.heightChanged || flags.dataChanged, this._showCenterLine);
                somethingChanged = true;
            }
            return somethingChanged;
        }
    }

    onStatisticsClicked (statisticsContainer: StatisticsContainer) {
        const length = statisticsContainer.variant.endIndex - statisticsContainer.variant.startIndex;
        const bubbleExpandFactor = 5;
        this.moveBrush({
            end: statisticsContainer.variant.endIndex + length / bubbleExpandFactor,
            start: statisticsContainer.variant.startIndex - length / bubbleExpandFactor
        });
    }

    onVariantContainerClicked (variantContainer: VariantContainer, position, sample) {
        const mapEndContainersFn = (m) => ({
            chromosome: m.chromosome || this.config.chromosome.name,
            chromosomeId: this.dataConfig.chromosomeId,
            position: m.displayPosition || m.position
        });
        const [endPoints] = variantContainer._endContainers
            .filter(m => m.visible === true)
            .map(mapEndContainersFn);
        const variantRequest = new EventVariationInfo(
            {
                chromosome: {
                    id: this.config.chromosome.id,
                    name: this.config.chromosome.name
                },
                endPoints,
                id: variantContainer._variant.identifier,
                position: variantContainer._variant.serverStartIndex,
                type: variantContainer._variant.type,
                sample,
                vcfFileId: this.dataConfig.id,
                projectId: this.config.projectId,
                projectIdNumber: this.config.project.id
            }
        );
        if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
            this.dataItemClicked(this, variantRequest, {name: 'variant-request', position});
        }
    }

    onClick({x, y}) {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            const checkPositionResult = this.renderer.checkPosition(this.viewport, this.cache,
                {x, y}, false);
            if (checkPositionResult && checkPositionResult.length > 0) {
                const variant = checkPositionResult[0].feature;
                const self = this;
                const mapFn = function (m) {
                    return {
                        chromosome: m.mate.chromosome,
                        chromosomeId: self.dataConfig.chromosomeId,
                        position: m.mate.position,
                        interChromosome: !m.mate.intraChromosome
                    };
                };
                const alts = variant.alternativeAllelesInfo.filter(x => x.mate).map(mapFn);
                if (alts.length === 0 && !variant.interChromosome) {
                    alts.push({
                        chromosome: this.config.chromosome.name,
                        chromosomeId: this.dataConfig.chromosomeId,
                        position: variant.endIndex,
                        interChromosome: false
                    });
                }
                const filterFn = a => a.interChromosome || a.position < self.viewport.brush.start || a.position > self.viewport.brush.end;
                const [endPoints] = alts.filter(filterFn);
                const variantRequest = new EventVariationInfo(
                    {
                        chromosome: {
                            id: this.config.chromosome.id,
                            name: this.config.chromosome.name
                        },
                        endPoints,
                        id: variant.identifier,
                        position: variant.serverStartIndex,
                        type: variant.type,
                        vcfFileId: this.dataConfig.id,
                        projectId: this.config.projectId,
                        projectIdNumber: this.config.project.id
                    }
                );
                if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                    this.dataItemClicked(this, variantRequest, {name: 'variant-request', position: {x, y}});
                }
            }
        } else {
            const variantContainer = this.renderer.onClick({x, y});
            if (variantContainer && variantContainer instanceof StatisticsContainer) {
                this.onStatisticsClicked(variantContainer);
            } else if (variantContainer instanceof VariantContainer) {
                this.onVariantContainerClicked(variantContainer, {x, y});
            }
        }
    }

    onHover({x, y}) {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            return super.onHover({x, y});
        }
        const hoveredItem = this.renderer.onMove({x, y});
        if (hoveredItem && !(hoveredItem instanceof StatisticsContainer) && this.shouldDisplayTooltips) {
            const tooltip = [
                ['Chromosome', this.config.chromosome.name],
                ['Start', hoveredItem.variant.startIndex],
                ['End', hoveredItem.variant.endIndex],
                ['ID', hoveredItem.variant.identifier],
                ['Type', hoveredItem.variant.type],
                ['REF', hoveredItem.variant.referenceAllele],
                ['ALT', hoveredItem.variant.alternativeAlleles.join(', ')],
                ['Quality', hoveredItem.variant.quality && hoveredItem.variant.quality.toFixed(2)]
            ];
            const filters = hoveredItem.variant.failedFilters || [];
            if (filters.length > 0) {
                tooltip.push(['Filter', filters.map(filter => filter.value).join(', ')]);
            }
            this.tooltip.setContent(tooltip);
            this.tooltip.move({x, y});
            this.tooltip.show({x, y});
        } else {
            this.tooltip.hide();
        }
        if (hoveredItem !== this._lastHovered) {
            this._lastHovered = hoveredItem;
            this.requestAnimation();
            return false;
        }
        return true;
    }

    unhoverRenderer () {
        this._lastHovered = null;
        if (this.state.variantsView === variantsView.variantsViewCollapsed) {
            this.renderer.onMove();
        }
    }

    onMouseOut() {
        super.onMouseOut();
        this.unhoverRenderer();
        // for correct animations
        this.requestAnimation();
    }

    animate(time) {
        if (this.state.variantsView === variantsView.variantsViewCollapsed) {
            return this.renderer.animate(time);
        }
        return false;
    }

    getTooltipDataObject(isHistogram, data) {
        if (isHistogram) {
            return [
                ['Count', data[0].value]
            ];
        } else if (data.length > 0) {
            const variant = data[0].feature;
            const tooltip = [
                ['Chromosome', this.config.chromosome.name],
                ['Start', variant.startIndex],
                ['End', variant.endIndex],
                ['ID', variant.identifier],
                ['Type', variant.type],
                ['REF', variant.referenceAllele],
                ['ALT', variant.alternativeAlleles.join(', ')],
                ['Quality', variant.quality && variant.quality.toFixed(2)]
            ];
            const filters = variant.failedFilters || [];
            if (filters.length > 0) {
                tooltip.push(['Filter', filters.map(filter => filter.value).join(', ')]);
            }
            return tooltip;
        }
    }

    prevVariation() {
        const position = Math.floor((this.viewport.brush.end + this.viewport.brush.start) / 2);
        this._dataService.getPreviousVariations(this.config, this.projectContext.currentChromosome.id, position - 1).then(
            (data) => {
                this.viewport.selectPosition(data.startIndex);
            }
        );
    }

    nextVariation() {
        const position = Math.floor((this.viewport.brush.end + this.viewport.brush.start) / 2);
        this._dataService.getNextVariations(this.config, this.projectContext.currentChromosome.id, position + 1).then(
            (data) => {
                this.viewport.selectPosition(data.startIndex);
            }
        );
    }

    clearData() {
        if (typeof this._removeHotKeyListener === 'function') {
            this._removeHotKeyListener();
        }
        super.clearData();
    }
}
