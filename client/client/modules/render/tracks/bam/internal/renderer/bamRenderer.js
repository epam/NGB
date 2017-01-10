import {
    AlignmentsRenderProcessor,
    BP_OFFSET,
    CAN_SHOW_DETAILS_FACTOR,
    CoverageRenderer,
    PlaceholderRenderer,
    renderCenterLine,
    renderDownSampleIndicators,
    renderGroups,
    renderScrollBar,
    renderSpliceJunctions
} from './features';
import CoverageArea from '../../../wig/wigArea.js';
import PIXI from 'pixi.js';
import {readsViewTypes} from '../../modes';

const Math = window.Math;
const DEFAULT_MAXIMUM_RANGE = 7000;

export const HOVERED_ITEM_TYPE_COVERAGE = 'coverage';
export const HOVERED_ITEM_TYPE_SPLICE_JUNCTION = 'splice junction';
export const HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR = 'downsample indicator';
export const HOVERED_ITEM_TYPE_ALIGNMENT = 'alignment';

export class BamRenderer {

    _alignmentsRenderProcessor: AlignmentsRenderProcessor = null;
    _coverageRenderer: CoverageRenderer = null;
    _coverageArea: CoverageArea = null;

    _placeholderRenderer: PlaceholderRenderer = null;

    _config = null;
    _viewport = null;
    _cacheService = null;

    _container = null;
    _placeholderContainer = null;
    _dataContainer = null;
    _coverageContainer = null;
    _centerLineGraphics = null;
    _downSampleGraphics = null;
    _spliceJunctionsGraphics = null;
    _scrollBarGraphics = null;
    _groupsBackground = null;
    _groupsNamesContainer = null;
    _groupsGraphics = null;

    _settings = null;
    _maximumRange = DEFAULT_MAXIMUM_RANGE;
    _noDataScaleFactor = 1;

    _height;
    _yPosition;
    _state;
    _scrollBarFrame = null;

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

    get maximumRange() {
        return this._maximumRange;
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
        if (this._viewport.actualBrushSize > this._maximumRange) {
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
        if (this._state.coverage) {
            margin += this._config.coverage.height;
        }
        if (this._state.spliceJunctions) {
            margin += this._config.spliceJunctions.height;
        }
        return margin;
    }

    get alignmentsRowHeight() {
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

    constructor(viewport, config, renderer, cacheService, opts) {
        this._config = config;
        this._viewport = viewport;
        this._pixiRenderer = renderer;
        this._cacheService = cacheService;
        this._settings = opts;
        this._maximumRange = opts.maxBAMBP || DEFAULT_MAXIMUM_RANGE;
        this._noDataScaleFactor = this._viewport.canvasSize / this._maximumRange;
        this._container = new PIXI.Container();
        this._placeholderContainer = new PIXI.Container();
        this._dataContainer = new PIXI.Container();
        this._container.addChild(this._dataContainer);
        this._container.addChild(this._placeholderContainer);
        this._yPosition = 0;
    }

    _initializeSubRenderers() {
        this._placeholderContainer.removeChildren();
        this._dataContainer.removeChildren();
        this._initGroupsBackground();
        this._initAlignments();
        this._initGroupsGraphics();
        this._initCoverage();
        this._initSpliceJunctionsGraphics();
        this._initDownsampleGraphics();
        this._initCenterLineGraphics();
        this._initScrollBarGraphics();
        this._initGroupNamesContainer();
        this._initPlaceholder();
    }

    globalSettingsChanged(newSettings) {
        this._settings = newSettings;
        const changed = this._maximumRange !== newSettings.maxBAMBP;
        this._maximumRange = newSettings.maxBAMBP || DEFAULT_MAXIMUM_RANGE;
        this._noDataScaleFactor = this._viewport.canvasSize / this._maximumRange;
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
            this._placeholderRenderer.init(this._maximumRange, {
                height: this._pixiRenderer.height,
                width: this._pixiRenderer.width
            });
        }

        this._dataContainer.visible = this._viewport.actualBrushSize <= this._maximumRange;
        this._placeholderContainer.visible = this._viewport.actualBrushSize > this._maximumRange;

        if (didSomethingChange && this._viewport.actualBrushSize <= this._maximumRange) {
            this._renderAlignments(flags);
            this._renderCoverage();
            this._renderGroups();
            renderDownSampleIndicators(this._cacheService.cache.downsampleCoverage, this._viewport, {
                config: this._config,
                graphics: this._downSampleGraphics,
                shouldRender: this._settings.isDownSampling,
                y: this.downsampleIndicatorsTopMargin
            });
            renderSpliceJunctions(this._cacheService.cache.spliceJunctions, this._viewport, {
                colors: this._settings.colors,
                config: this._config,
                graphics: this._spliceJunctionsGraphics,
                shouldRender: this._state.spliceJunctions,
                y: this.spliceJunctionsTopMargin
            });
            renderCenterLine(this._viewport, {
                config: this._config,
                graphics: this._centerLineGraphics,
                height: this._height,
                shouldRender: this._settings.showCenterLine
            });
            const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight;
            this._scrollBarFrame = renderScrollBar(
                {height: this._height, width: this._viewport.canvasSize},
                {end: this._yPosition + visibleLinesCount, start: this._yPosition, total: this._cacheService.cache.linesCount},
                {config: this._config, graphics: this._scrollBarGraphics, isHovered: false, topMargin: this.alignmentsDrawingTopMargin}
            );
        }

        return didSomethingChange;
    }

    _setScrollBarHoverStatus(hover) {
        const visibleLinesCount = (this._height - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight;
        this._scrollBarFrame = renderScrollBar(
            {height: this._height, width: this._viewport.canvasSize},
            {end: this._yPosition + visibleLinesCount, start: this._yPosition, total: this._cacheService.cache.linesCount},
            {config: this._config, graphics: this._scrollBarGraphics, isHovered: hover, topMargin: this.alignmentsDrawingTopMargin}
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
        this._coverageRenderer = new CoverageRenderer(this._config.coverage, this._config);
        this._coverageArea = new CoverageArea(this._viewport, this._config.coverage);
        this._coverageContainer.addChild(this._coverageRenderer.container);
        this._coverageContainer.addChild(this._coverageArea.axis);
    }

    _initSpliceJunctionsGraphics() {
        this._spliceJunctionsGraphics = new PIXI.Graphics();
        this._dataContainer.addChild(this._spliceJunctionsGraphics);
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

    _initPlaceholder() {
        this._placeholderRenderer = new PlaceholderRenderer();
        this._placeholderRenderer.init(this._maximumRange, {height: this._pixiRenderer.height, width: this._pixiRenderer.width});
        this._placeholderContainer.addChild(this._placeholderRenderer.container);
    }

    _renderGroups() {
        this._groupsBackground.removeChildren();
        this._groupsNamesContainer.removeChildren();
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

    _renderAlignments(flags) {
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

    _renderCoverage() {
        this._coverageContainer.visible = this._state.coverage;
        if (this._state.coverage) {
            this._cacheService.transform(this._viewport);
            this._coverageArea.render(this._viewport, this._cacheService.cache.coverage.coordinateSystem);
            this._coverageRenderer.render(this._viewport, this._cacheService.cache.coverage, false);
        }
    }

    _checkCoverage({x, y}) {
        if (y <= this.spliceJunctionsTopMargin) {
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
        }
        return null;
    }

    _checkSpliceJunction({x, y}) {
        const xBp = (this._viewport.project.pixel2brushBP(x) + BP_OFFSET) >> 0;
        if (y >= this.spliceJunctionsTopMargin && y <= this.downsampleIndicatorsTopMargin) {
            let spliceJunctionItem = null;
            const centerLine = this.spliceJunctionsTopMargin + this._config.spliceJunctions.height / 2;
            for (let i = 0; i < this._cacheService.cache.spliceJunctions.length; i++) {
                if (this._cacheService.cache.spliceJunctions[i].start <= xBp &&
                    this._cacheService.cache.spliceJunctions[i].end >= xBp) {
                    if ((!this._cacheService.cache.spliceJunctions[i].strand && y > centerLine) ||
                        (this._cacheService.cache.spliceJunctions[i].strand && y <= centerLine)) {
                        spliceJunctionItem = this._cacheService.cache.spliceJunctions[i];
                        break;
                    }
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
        const xBp = (this._viewport.project.pixel2brushBP(x) + BP_OFFSET) >> 0;
        const line = ((y - this.alignmentsDrawingTopMargin) / this.alignmentsRowHeight + this._yPosition);
        const alignment = AlignmentsRenderProcessor.checkAlignment({line, x: xBp}, this._cacheService.cache);
        if (alignment) {
            return {
                item: alignment,
                type: HOVERED_ITEM_TYPE_ALIGNMENT
            };
        }
        return null;
    }

    checkFeature({x, y}) {
        if (this._viewport.actualBrushSize > this._maximumRange) {
            return null;
        }
        return this._checkCoverage({x, y}) ||
            this._checkSpliceJunction({x, y}) ||
            this._checkDownsampleIndicator({x, y}) ||
            this._checkAlignment({x, y});
    }
}