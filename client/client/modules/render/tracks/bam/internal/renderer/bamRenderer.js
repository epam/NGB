import {
    AlignmentsRenderProcessor,
    BP_OFFSET,
    CAN_SHOW_DETAILS_FACTOR,
    CoverageRenderer,
    RegionsRenderer,
    PlaceholderRenderer,
    SashimiRenderer,
    renderDownSampleIndicators,
    renderGroups,
    renderScrollBar,
    renderSpliceJunctions
} from './features';
import {renderCenterLine} from '../../../../utilities';
import CoverageArea from '../../../wig/wigArea.js';
import PIXI from 'pixi.js';
import {readsViewTypes} from '../../modes';

const Math = window.Math;
const DEFAULT_MAXIMUM_RANGE = 7000;

export const HOVERED_ITEM_TYPE_COVERAGE = 'coverage';
export const HOVERED_ITEM_TYPE_REGION = 'region';
export const HOVERED_ITEM_TYPE_SPLICE_JUNCTION = 'splice junction';
export const HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR = 'downsample indicator';
export const HOVERED_ITEM_TYPE_ALIGNMENT = 'alignment';

export class BamRenderer {

    _alignmentsRenderProcessor: AlignmentsRenderProcessor = null;
    _coverageRenderer: CoverageRenderer = null;
    _sashimiRenderer: SashimiRenderer = null;
    _regionsRenderer: RegionsRenderer = null;
    _coverageArea: CoverageArea = null;
    _sashimiArea: CoverageArea = null;

    _zoomInPlaceholderRenderer: PlaceholderRenderer = null;
    _noReadsInRangePlaceholderRenderer: PlaceholderRenderer = null;

    _config = null;
    _viewport = null;
    _cacheService = null;

    _container = null;
    _zoomInPlaceholderContainer = null;
    _noReadsInRangePlaceholderContainer = null;
    _dataContainer = null;
    _coverageContainer = null;
    _sashimiContainer = null;
    _sashimiLabelsContainer = null;
    _centerLineGraphics = null;
    _downSampleGraphics = null;
    _spliceJunctionsGraphics = null;
    _scrollBarGraphics = null;
    _groupsBackground = null;
    _groupsNamesContainer = null;
    _groupsGraphics = null;
    _hoveredItemContainer = null;

    _settings = null;
    _maximumAlignmentsRange = DEFAULT_MAXIMUM_RANGE;
    _maximumCoverageRange = DEFAULT_MAXIMUM_RANGE;
    _noDataScaleFactor = 1;

    _height;
    _yPosition;
    _state;
    _scrollBarFrame = null;

    _lastHoveredFeature = null;

    set height(value) {
        this._height = value;
    }

    get container() {
        return this._container;
    }

    get scrollBarFrame() {
        return this._scrollBarFrame;
    }

    get isScrollable() {
        return this._scrollBarFrame !== null;
    }

    get maximumAlignmentsRange() {
        return this._maximumAlignmentsRange;
    }

    get maximumCoverageRange() {
        return Math.max(this._maximumCoverageRange, this.maximumAlignmentsRange);
    }

    get maximumRange() {
        return Math.max(this.maximumAlignmentsRange, this.maximumCoverageRange);
    }

    set yPosition(value) {
        if (value === undefined || value === null)
            value = this._yPosition;
        const rowHeight = this.alignmentsRowHeight;
        const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / rowHeight;
        const linesCount = this._cacheService.cache.linesCount;
        this._yPosition = Math.min(Math.max(value, 0), Math.max(0, linesCount - visibleLinesCount));
    }

    canSetYPosition(value) {
        if (this._viewport.actualBrushSize > this.maximumAlignmentsRange) {
            return false;
        }
        const rowHeight = this.alignmentsRowHeight;
        const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / rowHeight;
        const linesCount = this._cacheService.cache.linesCount;
        const newY = this._yPosition - value / rowHeight;
        return newY >= 0 && newY <= linesCount - visibleLinesCount;
    }

    setYPositionFromCanvasPosition(canvasY) {
        const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight;
        const linesCount = this._cacheService.cache.linesCount;
        this.yPosition = (canvasY - this.alignmentsDrawingTopMargin) * linesCount / visibleLinesCount / this.alignmentsRowHeight;
    }

    setScrollPosition({delta}) {
        this.yPosition = this._yPosition - delta / this.alignmentsRowHeight;
    }

    get downsampleIndicatorsTopMargin() {
        let margin = 0;
        if (this._state.coverage) {
            margin += this._config.coverage.height;
        }
        if (this._state.spliceJunctions) {
            margin += this._config.spliceJunctions.height;
        }
        return margin;
    }

    get spliceJunctionsTopMargin() {
        let margin = 0;
        if (this._state.coverage) {
            margin += this._config.coverage.height;
        }
        return margin;
    }

    get alignmentsDrawingTopMargin() {
        let margin = 0;
        if (this._settings.isDownSampling) {
            margin += this._config.downSampling.area.height;
        }
        if (this._state && this._state.coverage) {
            margin += this._config.coverage.height;
        }
        if (this._state && this._state.spliceJunctions) {
            margin += this._config.spliceJunctions.height;
        }
        return margin;
    }

    get alignmentsRowHeight() {
        if (this._state) {
            switch (this._state.readsViewMode) {
                case readsViewTypes.readsViewCollapsed:
                    return 1;
                case readsViewTypes.readsViewExpanded:
                    return this._config.yScale;
                case readsViewTypes.readsViewAutomatic: {
                    const x1 = 1 / CAN_SHOW_DETAILS_FACTOR;
                    const x2 = 1 / this._noDataScaleFactor;
                    const x = 1 / this._viewport.factor;
                    const scale = (x2 - x) / (x2 - x1);

                    return Math.max(1, Math.floor(1 + (this._config.yScale - 1) * scale));
                }
                default:
                    return this._config.yScale;
            }
        }
        return this._config.yScale;
    }

    constructor(viewport, config, renderer, cacheService, opts) {
        this._config = config;
        this._viewport = viewport;
        this._pixiRenderer = renderer;
        this._cacheService = cacheService;
        this._settings = opts;
        this._maximumAlignmentsRange = opts.maxBAMBP || DEFAULT_MAXIMUM_RANGE;
        this._maximumCoverageRange = opts.maxBAMCoverageBP || DEFAULT_MAXIMUM_RANGE;
        this._noDataScaleFactor = this._viewport.canvasSize / this.maximumAlignmentsRange;
        this._container = new PIXI.Container();
        this._zoomInPlaceholderContainer = new PIXI.Container();
        this._noReadsInRangePlaceholderContainer = new PIXI.Container();
        this._hoveredItemContainer = new PIXI.Container();
        this._dataContainer = new PIXI.Container();
        this._container.addChild(this._dataContainer);
        this._container.addChild(this._zoomInPlaceholderContainer);
        this._container.addChild(this._noReadsInRangePlaceholderContainer);
        this._yPosition = 0;
        this._groupAutoScaleManager = opts.groupAutoScaleManager;
    }

    _initializeSubRenderers() {
        this._zoomInPlaceholderContainer.removeChildren();
        this._noReadsInRangePlaceholderContainer.removeChildren();
        this._dataContainer.removeChildren();
        this._initGroupsBackground();
        this._initAlignments();
        this._initGroupsGraphics();
        this._initRegions();
        this._initCoverage();
        this._initSpliceJunctionsGraphics();
        this._initDownsampleGraphics();
        this._initCenterLineGraphics();
        this._initScrollBarGraphics();
        this._initGroupNamesContainer();
        this._initPlaceholder();
        this._dataContainer.addChild(this._hoveredItemContainer);
    }

    globalSettingsChanged(newSettings) {
        this._settings = newSettings;
        const changed = this._maximumAlignmentsRange !== newSettings.maxBAMBP || this._maximumCoverageRange !== newSettings.maxBAMCoverageBP;
        this._maximumAlignmentsRange = newSettings.maxBAMBP || DEFAULT_MAXIMUM_RANGE;
        this._maximumCoverageRange = newSettings.maxBAMCoverageBP || DEFAULT_MAXIMUM_RANGE;
        this._noDataScaleFactor = this._viewport.canvasSize / this.maximumAlignmentsRange;
        return changed;
    }

    _flagsChanged(flags) {
        return flags.renderReset
            || flags.widthChanged
            || flags.heightChanged
            || flags.brushChanged
            || flags.dataChanged
            || flags.renderFeaturesChanged
            || flags.textureCacheUpdated
            || flags.dragFinished
            || this._alignmentsRenderProcessor.renderedPosition === null
            || this._alignmentsRenderProcessor.renderedPosition !== this._yPosition;
    }

    render(flags, features) {
        this._state = features;
        if (flags.renderReset) {
            this._initializeSubRenderers();
        }
        const didSomethingChange = this._flagsChanged(flags);

        if (flags.widthChanged) {
            this._zoomInPlaceholderRenderer.init(this._getZoomInPlaceholderText(), {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        }

        const {rangeIsEmpty, noRegions} = this._cacheService ? this._cacheService.cache.rangeIsEmpty(this._viewport) : true;

        this._dataContainer.visible = !rangeIsEmpty || !noRegions;
        this._noReadsInRangePlaceholderContainer.visible = rangeIsEmpty && !this._cacheService.cache.isUpdating && this._viewport.actualBrushSize <= this.maximumRange && features.alignments;
        this._zoomInPlaceholderContainer.visible = this._viewport.actualBrushSize > this.maximumAlignmentsRange && !this._noReadsInRangePlaceholderContainer.visible && features.alignments;

        this._renderPlaceholders();

        if (didSomethingChange) {
            this._renderAlignments(flags, features);
            this._renderCoverage(flags, features);
            this._renderGroups(features);
            renderDownSampleIndicators(this._cacheService.cache.downsampleCoverage, this._viewport, {
                config: this._config,
                graphics: this._downSampleGraphics,
                shouldRender: this._settings.isDownSampling,
                y: this.downsampleIndicatorsTopMargin
            }, features, this.maximumAlignmentsRange);
            this._renderSpliceJunctions(flags, features);
            renderCenterLine(this._viewport, {
                config: this._config,
                graphics: this._centerLineGraphics,
                height: this._height,
                shouldRender: this._settings.showCenterLine && (features.coverage || features.alignments || features.spliceJunctions) && !rangeIsEmpty
            });
            const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight;
            this._scrollBarFrame = renderScrollBar(
                {height: this._height, width: this._viewport.canvasSize},
                {end: this._yPosition + visibleLinesCount, start: this._yPosition, total: this._cacheService.cache.linesCount},
                {config: this._config, graphics: this._scrollBarGraphics, isHovered: false, topMargin: this.alignmentsDrawingTopMargin},
                features, this._viewport, this.maximumAlignmentsRange
            );
            this.hoverFeature(this._lastHoveredFeature);
        }

        return didSomethingChange || flags.hoverChanged;
    }

    _setScrollBarHoverStatus(hover) {
        const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight;
        this._scrollBarFrame = renderScrollBar(
            {height: this._height, width: this._viewport.canvasSize},
            {end: this._yPosition + visibleLinesCount, start: this._yPosition, total: this._cacheService.cache.linesCount},
            {config: this._config, graphics: this._scrollBarGraphics, isHovered: hover, topMargin: this.alignmentsDrawingTopMargin},
            this._state, this._viewport, this.maximumAlignmentsRange
        );
    }

    hoverScrollBar() {
        this._setScrollBarHoverStatus(true);
    }

    unhoverScrollBar() {
        this._setScrollBarHoverStatus(false);
    }

    _initGroupsBackground() {
        this._groupsBackground = new PIXI.Container();
        this._dataContainer.addChild(this._groupsBackground);
    }

    _initGroupsGraphics() {
        this._groupsGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._groupsGraphics);
    }

    _initGroupNamesContainer() {
        this._groupsNamesContainer = new PIXI.Container();
        this._dataContainer.addChild(this._groupsNamesContainer);
    }

    _initAlignments() {
        this._alignmentsRenderProcessor = new AlignmentsRenderProcessor(this._pixiRenderer);
        this._dataContainer.addChild(this._alignmentsRenderProcessor.container);
    }

    _initCoverage() {
        this._dataContainer.addChild(this._coverageContainer = new PIXI.Container());
        this._coverageRenderer = new CoverageRenderer(this._config.coverage, this._config, this._state);
        this._coverageArea = new CoverageArea(this._viewport, this._config.coverage);
        this._coverageArea.registerGroupAutoScaleManager(this._groupAutoScaleManager);
        this._coverageContainer.addChild(this._coverageRenderer.container);
        this._coverageContainer.addChild(this._coverageArea.logScaleIndicator);
        this._coverageContainer.addChild(this._coverageArea.groupAutoScaleIndicator);
        this._coverageContainer.addChild(this._coverageArea.axis);
    }

    _initRegions() {
        this._dataContainer.addChild(this._regionsContainer = new PIXI.Container());
        this._regionsRenderer = new RegionsRenderer(this._config.regions);
        this._regionsRenderer.height = this._config.coverage.height;
        this._regionsContainer.addChild(this._regionsRenderer.container);
    }

    _initSpliceJunctionsGraphics() {
        this._spliceJunctionsGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._sashimiContainer = new PIXI.Container());
        this._dataContainer.addChild(this._spliceJunctionsGraphics);
        this._sashimiRenderer = new SashimiRenderer(this._config.spliceJunctions.sashimi, this._config);
        this._sashimiArea = new CoverageArea(this._viewport, this._config.spliceJunctions.sashimi.coverage);
        this._sashimiContainer.addChild(this._sashimiRenderer.container);
        this._sashimiContainer.addChild(this._sashimiArea.axis);
        this._sashimiLabelsContainer = new PIXI.Container();
        this._dataContainer.addChild(this._sashimiLabelsContainer);
    }

    _initDownsampleGraphics() {
        this._downSampleGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._downSampleGraphics);
    }

    _initCenterLineGraphics() {
        this._centerLineGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._centerLineGraphics);
    }

    _initScrollBarGraphics() {
        this._scrollBarGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._scrollBarGraphics);
    }

    _getZoomInPlaceholderText() {
        const unitThreshold = 1000;
        const noReadText = {
            unit: this.maximumAlignmentsRange < unitThreshold ? 'BP' : 'kBP',
            value: this.maximumAlignmentsRange < unitThreshold ? this.maximumAlignmentsRange : Math.ceil(this.maximumAlignmentsRange / unitThreshold)
        };
        return `Zoom in to see reads.
Minimal zoom level is at ${noReadText.value}${noReadText.unit}`;
    }

    _initPlaceholder() {
        this._zoomInPlaceholderRenderer = new PlaceholderRenderer();
        this._zoomInPlaceholderRenderer.init(this._getZoomInPlaceholderText(), {height: this._pixiRenderer.height, width: this._pixiRenderer.width});
        this._zoomInPlaceholderContainer.addChild(this._zoomInPlaceholderRenderer.container);

        this._noReadsInRangePlaceholderRenderer = new PlaceholderRenderer();
        this._noReadsInRangePlaceholderRenderer.init('No reads in area', {height: this._pixiRenderer.height, width: this._pixiRenderer.width});
        this._noReadsInRangePlaceholderContainer.addChild(this._noReadsInRangePlaceholderRenderer.container);
    }

    _renderPlaceholders() {
        const topMargin = this.downsampleIndicatorsTopMargin;
        this._zoomInPlaceholderRenderer.init(this._getZoomInPlaceholderText(),
            {
                height: this._pixiRenderer.height - topMargin, width: this._pixiRenderer.width
            });
        this._zoomInPlaceholderContainer.y = topMargin;
    }

    _renderGroups(features) {
        this._groupsBackground.removeChildren();
        this._groupsNamesContainer.removeChildren();
        if (features.alignments && this._viewport.actualBrushSize <= this.maximumAlignmentsRange) {
            renderGroups(this._state, this._cacheService.cache.groups,
                {
                    alignmentRowHeight: this.alignmentsRowHeight,
                    config: this._config,
                    groupNamesContainer: this._groupsNamesContainer,
                    groupsBackground: this._groupsBackground,
                    groupsSeparatorGraphics: this._groupsGraphics,
                    height: this._height,
                    scrollY: this._yPosition,
                    topMargin: this.alignmentsDrawingTopMargin,
                    viewport: this._viewport
                });
        }
    }

    _renderAlignments(flags, features) {
        this._alignmentsRenderProcessor.container.visible = features.alignments && this._viewport.actualBrushSize <= this.maximumAlignmentsRange;
        if (!features.alignments || this._viewport.actualBrushSize > this.maximumAlignmentsRange) {
            this._alignmentsRenderProcessor.clear();
            return;
        }
        this._alignmentsRenderProcessor.alignmentHeight = this.alignmentsRowHeight;
        this._alignmentsRenderProcessor.render(
            this._cacheService.cache,
            this._viewport,
            flags,
            {
                colors: this._settings.colors,
                config: this._config,
                currentY: this._yPosition,
                features: this._state,
                height: this._height,
                renderer: this._pixiRenderer,
                topMargin: this.alignmentsDrawingTopMargin
            }
        );
    }

    _renderCoverage(flags, features) {
        this._coverageContainer.visible = this._state.coverage && this._viewport.actualBrushSize <= this.maximumCoverageRange;
        this._regionsContainer.visible = this._state.coverage && this._viewport.actualBrushSize > this.maximumCoverageRange;
        if (this._state.coverage && this._viewport.actualBrushSize <= this.maximumCoverageRange) {
            if (flags.dataChanged || flags.dragFinished) {
                this._cacheService.transform(this._viewport, features);
            }
            this._coverageArea.render(this._viewport, this._cacheService.cache.coverage.coordinateSystem, features);
            this._coverageRenderer.render(this._viewport, this._cacheService.cache.coverage, false);
        }
        if (this._state.coverage && this._viewport.actualBrushSize > this.maximumCoverageRange) {
            this._regionsRenderer.render(this._viewport, this._cacheService.cache.regionItems);
        }
    }

    _renderSpliceJunctions(flags, features) {
        this._sashimiContainer.visible = features.sashimi;
        this._sashimiLabelsContainer.removeChildren();
        if (features.sashimi) {
            this._sashimiArea.height = this._height / 2.0;
            this._sashimiRenderer.height = this._height / 2.0;
            this._sashimiArea.render(
                this._viewport,
                this._cacheService.cache.coverage.coordinateSystem,
                features
            );
            this._sashimiRenderer.render(
                this._viewport,
                this._cacheService.cache,
                false,
                {
                    graphics: this._spliceJunctionsGraphics,
                    labelsContainer: this._sashimiLabelsContainer,
                    shouldRender: this._viewport.actualBrushSize <= this.maximumAlignmentsRange,
                    spliceJunctionsFiltering: features.spliceJunctionsFiltering,
                    y: this.spliceJunctionsTopMargin
                }
            );
        } else {
            this._sashimiContainer.visible = false;
            renderSpliceJunctions(
                this._cacheService.cache.spliceJunctions,
                this._viewport,
                {
                    colors: this._settings.colors,
                    config: this._config.spliceJunctions,
                    graphics: this._spliceJunctionsGraphics,
                    sashimi: features.sashimi,
                    shouldRender: features.spliceJunctions && this._viewport.actualBrushSize <= this.maximumAlignmentsRange,
                    spliceJunctionsFiltering: features.spliceJunctionsFiltering,
                    y: this.spliceJunctionsTopMargin
                });
        }
    }

    _checkCoverage({x, y}) {
        if (y <= this.spliceJunctionsTopMargin) {
            if (this._state.coverage && this._viewport.actualBrushSize <= this.maximumCoverageRange) {
                const coverageItem = this._coverageRenderer.onMove(this._viewport, {
                    x: x,
                    y: this.spliceJunctionsTopMargin - y
                }, this._cacheService.cache.coverage.data);
                if (coverageItem) {
                    return {
                        item: coverageItem,
                        type: HOVERED_ITEM_TYPE_COVERAGE
                    };
                }
            } else if (this._state.coverage && this._viewport.actualBrushSize > this.maximumCoverageRange) {
                for (let i = 0; i < this._cacheService.cache.regionItems.length; i++) {
                    const item = this._cacheService.cache.regionItems[i];
                    if (this._viewport.project.brushBP2pixel(item.startIndex) <= x &&
                        this._viewport.project.brushBP2pixel(item.endIndex) >= x) {
                        return {
                            item: item,
                            type: HOVERED_ITEM_TYPE_REGION
                        };
                    }
                }
            }
        }
        return null;
    }

    _checkSpliceJunction({x, y}) {
        const xBp = (this._viewport.project.pixel2brushBP(x) + BP_OFFSET) >> 0;
        const filter = this._state.spliceJunctionsFiltering || 0;
        if (y >= this.spliceJunctionsTopMargin && y <= this.downsampleIndicatorsTopMargin) {
            let spliceJunctionItem = null;
            const centerLine = this.spliceJunctionsTopMargin + this._config.spliceJunctions.height / 2;
            for (let i = 0; i < this._cacheService.cache.spliceJunctions.length; i++) {
                if (
                    this._cacheService.cache.spliceJunctions[i].start <= xBp &&
                    this._cacheService.cache.spliceJunctions[i].end >= xBp &&
                    this._cacheService.cache.spliceJunctions[i].count >= filter &&
                    (
                        (!this._cacheService.cache.spliceJunctions[i].strand && y > centerLine) ||
                        (this._cacheService.cache.spliceJunctions[i].strand && y <= centerLine)
                    )
                ) {
                    spliceJunctionItem = this._cacheService.cache.spliceJunctions[i];
                    break;
                }
            }
            if (spliceJunctionItem) {
                return {
                    item: spliceJunctionItem,
                    type: HOVERED_ITEM_TYPE_SPLICE_JUNCTION
                };
            }
        }
        return null;
    }

    _checkDownsampleIndicator({x, y}) {
        const xBp = (this._viewport.project.pixel2brushBP(x) + BP_OFFSET) >> 0;
        if (y >= this.downsampleIndicatorsTopMargin && y <= this.alignmentsDrawingTopMargin) {
            let downsampleIndicatorItem = null;
            for (let i = 0; i < this._cacheService.cache.downsampleCoverage.length; i++) {
                if (this._cacheService.cache.downsampleCoverage[i].startIndex <= xBp &&
                    this._cacheService.cache.downsampleCoverage[i].endIndex >= xBp) {
                    downsampleIndicatorItem = this._cacheService.cache.downsampleCoverage[i];
                    break;
                }
            }
            if (downsampleIndicatorItem) {
                return {
                    item: downsampleIndicatorItem,
                    type: HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR
                };
            }
        }
        return null;
    }

    _checkAlignment({x, y}) {
        if (!this._state.alignments || this._viewport.actualBrushSize > this.maximumAlignmentsRange) {
            return null;
        }
        const xBp = (this._viewport.project.pixel2brushBP(x) + BP_OFFSET) >> 0;
        const line = ((y - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight + this._yPosition);
        const alignment = AlignmentsRenderProcessor.checkAlignment({line, x: xBp}, this._cacheService.cache);
        if (alignment) {
            return {
                item: alignment,
                line: line >> 0,
                type: HOVERED_ITEM_TYPE_ALIGNMENT
            };
        }
        return null;
    }

    checkFeature({x, y}) {
        if (this._viewport.actualBrushSize > this.maximumRange) {
            return this._checkCoverage({x, y});
        }
        return this._checkCoverage({x, y}) ||
            this._checkSpliceJunction({x, y}) ||
            this._checkDownsampleIndicator({x, y}) ||
            this._checkAlignment({x, y});
    }

    hoverFeature(feature) {
        this._coverageRenderer && this._coverageRenderer.hoverItem(null);
        this._hoveredItemContainer && this._hoveredItemContainer.removeChildren();
        if (feature) {
            switch (feature.type) {
                case HOVERED_ITEM_TYPE_COVERAGE: {
                    if (this._coverageRenderer) {
                        this._coverageRenderer
                            .hoverItem(
                                feature.item,
                                this._viewport,
                                this._cacheService.cache.coverage.data,
                                this._cacheService.cache.coverage.coordinateSystem
                            );
                    }
                } break;
                case HOVERED_ITEM_TYPE_REGION: {
                    if (this._regionsRenderer) {
                        this._regionsRenderer.render(
                            this._viewport,
                            this._cacheService.cache.regionItems,
                            feature.item
                        );
                    }
                } break;
                case HOVERED_ITEM_TYPE_SPLICE_JUNCTION: {
                    if (!this._state.sashimi) {
                        const graphics = new PIXI.Graphics();
                        renderSpliceJunctions(
                            this._cacheService.cache.spliceJunctions,
                            this._viewport,
                            {
                                colors: this._settings.colors,
                                config: this._config.spliceJunctions,
                                graphics: graphics,
                                hovered: feature.item,
                                sashimi: this._state.sashimi,
                                shouldRender: this._state.spliceJunctions && this._viewport.actualBrushSize <= this.maximumAlignmentsRange,
                                spliceJunctionsFiltering: this._state.spliceJunctionsFiltering,
                                y: this.spliceJunctionsTopMargin
                            });
                        this._hoveredItemContainer.addChild(graphics);
                    }
                } break;
                case HOVERED_ITEM_TYPE_ALIGNMENT: {
                    this._alignmentsRenderProcessor.hoverRead(
                        this._cacheService.cache,
                        this._viewport,
                        {
                            colors: this._settings.colors,
                            config: this._config,
                            currentY: this._yPosition,
                            features: this._state,
                            height: this._height,
                            renderer: this._pixiRenderer,
                            topMargin: this.alignmentsDrawingTopMargin
                        }, this._hoveredItemContainer, feature.item, feature.line
                    );
                } break;
            }
        } else {
            if (this._alignmentsRenderProcessor && this._alignmentsRenderProcessor.container) {
                this._alignmentsRenderProcessor.container.visible = true;
            }
            if (this._lastHoveredFeature && this._lastHoveredFeature.type === HOVERED_ITEM_TYPE_REGION) {
                this._regionsRenderer.render(this._viewport, this._cacheService.cache.regionItems);
            }
        }

        const hoveredFeatureChanged = ((feature && !this._lastHoveredFeature) || (!feature && this._lastHoveredFeature));
        this._lastHoveredFeature = feature;
        return hoveredFeatureChanged;
    }
}
