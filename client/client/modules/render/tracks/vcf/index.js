import {StatisticsContainer, VariantContainer, VCFCollapsedRenderer, VCFExpandedRenderer, VcfTransformer} from './internal';
import {GENETrack} from '../gene';
import VcfConfig from './vcfConfig';
import {VcfDataService} from '../../../../dataServices';
import GeneConfig from '../gene/geneConfig';
import {default as menu} from './menu';
import {variantsView} from './modes';
import {menu as menuUtilities} from '../../utilities';
import {EventVariationInfo} from '../../../../app/shared/utils/events';

export class VCFTrack extends GENETrack{

    static trackDefaultHeight = VcfConfig.height;
    trackLocalConfig = Object.assign(GeneConfig, VcfConfig);
    _collapsedRenderer: VCFCollapsedRenderer = null;
    _lastHovered = null;

    projectContext;

    get stateKeys() {
        return ['variantsView'];
    }

    constructor(opts) {
        super(opts);
        this.transformer.collapsed = this.state.variantsView === variantsView.variantsViewCollapsed;

        this._actions = [
            {
                enabled: function() {return true;},
                label: 'Navigation',
                type: 'groupLinks',
                links: [{
                    label: 'Prev',
                    handleClick: ::this.prevVariation
                },{
                    label: 'Next',
                    handleClick: ::this.nextVariation
                }]
            }
        ];
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        const wrapStateFn = (fn) => () => fn(this.state);
        const wrapMutatorFn = (fn) => () => {
            const oldVariantsView = this.state.variantsView;
            fn(this.state);
            this.transformer.collapsed = this.state.variantsView === variantsView.variantsViewCollapsed;
            if (oldVariantsView !== this.state.variantsView) {
                this._flags.renderReset = true;
            }
            this.updateAndRefresh();
            this.reportTrackState();
        };

        this._menu = menu.map(function processMenuList(menuEntry) {
            const result = {};
            for (const key of Object.keys(menuEntry)) {
                switch (true) {
                    case Array.isArray(menuEntry[key]):
                        result[key] = menuEntry[key].map(processMenuList);
                        break;
                    case menuEntry[key] instanceof Function: {
                        switch (true) {
                            case key.startsWith('is'):
                                result[key] = wrapStateFn(menuEntry[key]);
                                break;
                            case key.startsWith('display'):
                                result[key] = wrapStateFn(menuEntry[key]);
                                break;
                            default:
                                result[key] = wrapMutatorFn(menuEntry[key]);
                                break;
                        }
                    }
                        break;
                    default:
                        result[key] = menuEntry[key];
                }
            }

            return result;
        });
        this.hotKeyListener = (event) => {
            if (event) {
                const path = event.split('>');
                if (path && path[0] === 'vcf') {
                    const menuItem = menuUtilities.findMenuItem(this._menu, event);
                    if (menuItem) {
                        if (menuItem.type === 'button') {
                            menuItem.perform();
                        }
                        else if (menuItem.type === 'checkbox') {
                            menuItem.isEnabled() ? menuItem.disable() : menuItem.enable();
                        }

                    }
                }
            }
        };
        const _hotKeyListener = ::this.hotKeyListener;
        const self = this;
        this._removeHotKeyListener = function() {
            self.dispatcher.removeListener('hotkeyPressed', _hotKeyListener);
        };
        this.dispatcher.on('hotkeyPressed', _hotKeyListener);

        return this._menu;
    }

    get transformer() {
        if (!this._transformer) {
            this._transformer = new VcfTransformer(this.trackLocalConfig, this.config.chromosome);
        }
        return this._transformer;
    }

    get dataService() {
        if (!this._dataService) {
            this._dataService = new VcfDataService();
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
            this._renderer = new VCFExpandedRenderer(this.trackLocalConfig, this.transformer);
        }
        if (!this._collapsedRenderer) {
            this._collapsedRenderer = new VCFCollapsedRenderer(VcfConfig);
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

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
        }
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
                this.renderer.render(this.viewport, this.cache, flags.heightChanged, this._gffColorByFeatureType);
                somethingChanged = true;
            }
            return somethingChanged;
        }
    }

    onClick({x, y}) {
        if (this.state.variantsView === variantsView.variantsViewExpanded) {
            const checkPositionResult = this.renderer.checkPosition(this.viewport, this.cache,
                {x, y}, false);
            if (checkPositionResult && checkPositionResult.length > 0) {
                const variant = checkPositionResult[0];
                const self = this;
                const mapFn = function(m) {
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
                        projectId: this.config.projectId
                    }
                );
                if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                    this.dataItemClicked(this, variantRequest, {name: 'variant-request', position: {x, y}});
                }
            }
        } else {
            const variantContainer = this.renderer.onClick({x, y});
            if (variantContainer && variantContainer instanceof StatisticsContainer) {
                const length = variantContainer.variant.endIndex - variantContainer.variant.startIndex;
                const bubbleExpandFactor = 5;
                this.moveBrush({
                    end: variantContainer.variant.endIndex + length / bubbleExpandFactor,
                    start: variantContainer.variant.startIndex - length / bubbleExpandFactor
                });
            }
            else if (variantContainer instanceof VariantContainer) {
                const self = this;
                const mapEndContainersFn = function (m) {
                    return {
                        chromosome: m.chromosome || self.config.chromosome.name,
                        chromosomeId: self.dataConfig.chromosomeId,
                        position: m.displayPosition || m.position
                    };
                };
                const [endPoints] = variantContainer._endContainers.filter(m=>m.visible === true).map(mapEndContainersFn);
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
                        vcfFileId: this.dataConfig.id,
                        projectId: this.config.projectId
                    }
                );
                if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                    this.dataItemClicked(this, variantRequest, {name: 'variant-request', position: {x, y}});
                }
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

    onMouseOut() {
        super.onMouseOut();
        this._lastHovered = null;
        if (this.state.variantsView === variantsView.variantsViewCollapsed) {
            this.renderer.onMove();
        }
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
            const variant = data[0];
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
        const position =  Math.floor((this.viewport.brush.end + this.viewport.brush.start)/2);
        this._dataService.getPreviousVariations(this.config, this.projectContext.currentChromosome.id, position - 1).then(
            (data) => {
                this.viewport.selectPosition(data.startIndex);
            }
        );
    }

    nextVariation() {
        const position =  Math.floor((this.viewport.brush.end + this.viewport.brush.start)/2);
        this._dataService.getNextVariations(this.config, this.projectContext.currentChromosome.id, position + 1).then(
            (data) => {
                this.viewport.selectPosition(data.startIndex);
            }
        );
    }
}