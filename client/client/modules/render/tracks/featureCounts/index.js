import FCRenderer from './internal/renderer/fcRenderer';
import FCBarChartRenderer from './internal/renderer/fcBarChartRenderer';
import {FeatureCountsTransformer} from './internal/data/featureCountsTransformer';
import {GENETrack} from '../gene';
import Menu from '../../core/menu';
import featureCountsMenu, {scaleModesStateMutators} from './menu';
import {displayModes} from './modes';
import FeatureCountsConfig from './featureCountsConfig';
import {DRAG_STAGE_DRAGGING, DRAG_STAGE_FINISHED, DRAG_STAGE_STARTED}
    from '../../core/track/interactiveTrack';

export class FeatureCountsTrack extends GENETrack {
    static getTrackDefaultConfig() {
        return FeatureCountsConfig;
    }

    static fcPostStateMutatorFn = (track, key, prePayload) => {
        if (prePayload.featureCountsDisplayMode !== track.state.featureCountsDisplayMode) {
            track.state.selectedSources = undefined;
            if (
                track.state.featureCountsDisplayMode === displayModes.barChart &&
                track.trackConfig &&
                track.trackConfig.barChart
            ) {
                const availableSources = track.cache && track.cache.sources
                    ? (track.cache.sources.sources || [])
                    : [];
                const subTracks = availableSources.length;
                const {
                    minimumHeight: subTrackMinimumHeight = 100
                } = track.trackConfig.barChart;
                const minimumHeight = subTracks * subTrackMinimumHeight;
                if (track.height < minimumHeight) {
                    track.height = minimumHeight;
                }
            }
            track.sourceTypesChanged && track.sourceTypesChanged();
        }
    };

    onClick({x, y}) {
        this.tooltip.hide();
        const isHistogram = this.transformer.isHistogramDrawingModeForViewport(this.viewport, this.cache);
        const checkPositionResult = this.getFeaturesByCoordinates({x, y});

        if (!isHistogram && checkPositionResult && checkPositionResult.length > 0) {
            if (this.dataItemClicked !== null && this.dataItemClicked !== undefined) {
                const feature = checkPositionResult[0].feature;
                const eventInfo = {
                    endIndex: feature.endIndex,
                    feature,
                    info: this.getTooltipDataObject(false, checkPositionResult),
                    startIndex: feature.startIndex
                };
                this.dataItemClicked(this, eventInfo, {name: 'feature-click', position: {x, y}});
            }
        }
    }

    static Menu = Menu(
        featureCountsMenu,
        {
            postStateMutatorFn: [
                scaleModesStateMutators.postStateMutatorFn,
                FeatureCountsTrack.fcPostStateMutatorFn,
                GENETrack.postStateMutatorFn
            ],
            preStateMutatorFn: scaleModesStateMutators.preStateMutatorFn,
            afterStateMutatorFn: scaleModesStateMutators.afterStateMutatorFn
        }
    );

    barChartRenderer = new FCBarChartRenderer(this, this.trackConfig, this._pixiRenderer);

    get stateKeys() {
        return [
            'header',
            'featureCountsDisplayMode',
            'singleBarChartColors',
            'grayScaleColors',
            'coverageLogScale',
            'coverageScaleMode',
            'coverageScaleFrom',
            'coverageScaleTo',
            'groupAutoScale'
        ];
    }

    get transformer () {
        if (!this._transformer) {
            this._transformer = new FeatureCountsTransformer(this.trackConfig);
        }
        return this._transformer;
    }

    get renderer(): FCRenderer {
        if (!this._renderer) {
            this._renderer = new FCRenderer(
                this.trackConfig,
                this.transformer,
                this
            );
        }
        return this._renderer;
    }

    get featuresFilteringEnabled () {
        return false;
    }

    get verticalScrollRenderer() {
        if (this.barChartDisplayMode) {
            return this.barChartRenderer;
        }
        return this.renderer;
    }

    get barChartDisplayMode () {
        return this.state.featureCountsDisplayMode === displayModes.barChart;
    }

    get trackHasCoverageSubTrack() {
        return this.barChartDisplayMode;
    }

    updateCacheData (data) {
        super.updateCacheData(data);
        if (this.cache) {
            delete this.cache.sources;
            this.cache.sources = this.transformer
                .getSourcesInfo(data, this.state.selectedSources) || {};
            this.state.sources = (this.cache.sources.sources || []).slice();
            if (this.fcSourcesManager) {
                this.fcSourcesManager.registerSource(...this.state.sources);
            }
        }
    }

    constructor(opts) {
        super(opts);
        this._actions = null;
        this.fcSourcesManager = opts.fcSourcesManager;
        this.transformer.registerGroupAutoScaleManager(opts.groupAutoScaleManager, this);
        this.barChartRenderer.registerGroupAutoScaleManager(opts.groupAutoScaleManager);
        this.barChartRenderer.registerFCSourcesManager(opts.fcSourcesManager);
    }

    sourceTypesChanged () {
        if (this.cache && this.cache.sources) {
            const {
                selectedSources = []
            } = this.state;
            this.barChartRenderer.visibleSources = selectedSources;
            Object.entries(this.cache.sources.values || {})
                .forEach(([source, o]) => {
                    o.disabled = selectedSources.length > 0 && !selectedSources.includes(source);
                });
        }
    }

    onDrag(...opts) {
        const [options] = opts;
        const {currentLocal, stage, startLocal} = options;
        let handled = false;
        if (this.barChartDisplayMode && this.barChartRenderer) {
            switch (stage) {
                case DRAG_STAGE_STARTED: {
                    const sourceToDrag = this.barChartRenderer
                        .checkSourceTitle(startLocal);
                    handled = this.barChartRenderer.startSourceMoving(sourceToDrag, currentLocal);
                    break;
                }
                case DRAG_STAGE_DRAGGING:
                    handled = this.barChartRenderer.moveSource(currentLocal);
                    break;
                case DRAG_STAGE_FINISHED:
                    handled = this.barChartRenderer.finishSourceMoving(currentLocal);
                    break;
            }
        }
        if (!handled) {
            return super.onDrag(...opts);
        } else {
            this._flags.dragFinished = true;
            this.requestRender();
            this.tooltip.hide();
            return true;
        }
    }

    getFeaturesByCoordinates ({x, y}) {
        if (this.barChartDisplayMode && this.barChartRenderer) {
            return this.barChartRenderer.checkPosition({x, y});
        }
        return super.getFeaturesByCoordinates({x, y});
    }

    hoverItem (items, isHistogram) {
        if (this.barChartDisplayMode && this.barChartRenderer) {
            return this.barChartRenderer.hoverItem(items, this.viewport, this.cache);
        }
        return super.hoverItem(items, isHistogram);
    }

    getTooltipDataObject(isHistogram, geneData) {
        if (this.barChartDisplayMode && !isHistogram && geneData && geneData.length > 0) {
            const [item] = geneData;
            if (item.sourceValue) {
                const info = super.getTooltipDataObject(isHistogram, geneData) || [];
                return [
                    ['Features count', item.sourceValue],
                    ...info
                ];
            }
        }
        return super.getTooltipDataObject(isHistogram, geneData);
    }

    render (flags) {
        let somethingChanged = super.render(flags);
        if (flags.renderReset) {
            this.container.addChild(this.renderer.container);
            this.container.addChild(this.barChartRenderer.container);
            somethingChanged = true;
        }
        if (
            flags.brushChanged ||
            flags.widthChanged ||
            flags.heightChanged ||
            flags.renderReset ||
            flags.dataChanged ||
            flags.dragFinished
        ) {
            this.renderer.height = this.height;
            this.barChartRenderer.height = this.height;
            if (this.barChartDisplayMode) {
                this.cache.coordinateSystem = this.transformer
                    .getCoordinateSystem(this.viewport, this.cache, this.state);
                this.barChartRenderer.showPlaceholder = this.transformer
                    .isHistogramDrawingModeForViewport(this.viewport, this.cache);
                this.barChartRenderer.grayScaleColors = this.state.grayScaleColors;
                this.barChartRenderer.singleColors = this.state.singleBarChartColors;
                this.barChartRenderer.visibleSources = this.state.selectedSources;
                this.barChartRenderer.render(
                    this.viewport,
                    this.cache,
                    flags.heightChanged || flags.dataChanged,
                    this._showCenterLine
                );
            } else {
                this.renderer.render(
                    this.viewport,
                    this.cache,
                    flags.heightChanged || flags.dataChanged,
                    this._showCenterLine,
                    this._gffColorByFeatureType,
                    false,
                    true
                );
            }
            somethingChanged = true;
        }
        this.renderer.container.visible = !this.barChartDisplayMode;
        this.barChartRenderer.container.visible = this.barChartDisplayMode;
        return somethingChanged;
    }
}
