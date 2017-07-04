import {
    BamCacheService,
    BamRenderer,
    CoverageTransformer,
    HOVERED_ITEM_TYPE_ALIGNMENT,
    HOVERED_ITEM_TYPE_COVERAGE,
    HOVERED_ITEM_TYPE_REGION,
    HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR,
    HOVERED_ITEM_TYPE_SPLICE_JUNCTION
} from './internal';
import {dataModes, groupModes, sortTypes} from './modes';
import Promise from 'bluebird';
import {ScrollableTrack} from '../../core';
import BAMConfig from './bamConfig';
import {default as menu} from './menu';
import {menu as menuUtilities} from '../../utilities';
import scaleModes from '../wig/modes';

const Math = window.Math;
export class BAMTrack extends ScrollableTrack {

    cacheService: BamCacheService = null;

    downsampled = false;
    shouldDisplayCenterLine = false;

    features = null;
    bamRequestSettings = null;
    bamRenderSettings = {};

    dispatcher = null;

    _bamRenderer: BamRenderer = null;

    static getTrackDefaultConfig() {
        return BAMConfig;
    }

    get stateKeys() {
        return [
            'arrows',
            'alignments',
            'colorMode',
            'coverage',
            'diffBase',
            'groupMode',
            'ins_del',
            'mismatches',
            'readsViewMode',
            'shadeByQuality',
            'softClip',
            'spliceJunctions',
            'viewAsPairs',
            'coverageLogScale',
            'coverageScaleMode',
            'coverageScaleFrom',
            'coverageScaleTo'];
    }

    get trackHasCoverageSubTrack() {
        return true;
    }

    get trackIsResizable() {
        if (this.state) {
            return this.state.alignments;
        }
        return true;
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        const wrapStateFn = (fn) => () => fn(this.state);

        const getCoverageExtremum = () => {
            let max = 0;
            let min = 0;
            if (this.cacheService && this.cacheService.cache && this.cacheService.cache.coverage && this.cacheService.cache.coverage.coordinateSystem) {
                max = this.cacheService.cache.coverage.coordinateSystem.realMaximum;
                min = this.cacheService.cache.coverage.coordinateSystem.realMinimum;
            } else {
                min = this.state.coverageScaleFrom;
                max = this.state.coverageScaleTo;
            }
            return {max, min};
        };

        const wrapMutatorFn = (fn, key) => () => {
            const viewAsPairsFlag = this.state.viewAsPairs;
            const groupingMode = this.state.groupMode;
            let shouldReportTracksState = true;
            const currentScaleMode = this.state.coverageScaleMode;
            const logScaleEnabled = this.state.coverageLogScale;
            const alignments = this.state.alignments;
            fn(this.state);
            if ((!this.state.alignments || this.state.alignments !== alignments) && this.changeTrackHeight) {
                if (this.state.alignments) {
                    this.changeTrackHeight(this.trackConfig.defaultHeight);
                } else {
                    let newHeight = 0;
                    if (this.state.coverage) {
                        newHeight += this.trackConfig.coverage.height;
                    }
                    if (this.state.spliceJunctions) {
                        newHeight += this.trackConfig.spliceJunctions.height;
                    }
                    this.changeTrackHeight(newHeight);
                }
            }
            if (key === 'coverage>scale>manual' && this.state.coverageScaleMode === scaleModes.manualScaleMode) {
                shouldReportTracksState = false;
                if (currentScaleMode !== this.state.coverageScaleMode) {
                    this.state.coverageScaleMode = scaleModes.defaultScaleMode;
                }
                this.config.dispatcher.emitSimpleEvent('tracks:coverage:manual:configure', {
                    source: this.config.name,
                    config: {
                        extremumFn: getCoverageExtremum,
                        isLogScale: this.state.coverageLogScale
                    }
                });
            } else if (currentScaleMode !== this.state.coverageScaleMode) {
                this._flags.dataChanged = true;
                this.state.coverageScaleFrom = undefined;
                this.state.coverageScaleTo = undefined;
            } else if (logScaleEnabled !== this.state.coverageLogScale) {
                this._flags.dataChanged = true;
            }
            const viewAsPairsFlagChanged = viewAsPairsFlag !== this.state.viewAsPairs;
            if (viewAsPairsFlagChanged) {
                if (this.state.groupMode === groupModes.groupByReadStrandMode && this.state.viewAsPairs) {
                    this.state.groupMode = groupModes.defaultGroupingMode;
                }
                this.bamRenderSettings.viewAsPairs = this.state.viewAsPairs;
            }
            const groupingModeChanged = groupingMode !== this.state.groupMode;
            if (groupingModeChanged) {
                if (this.state.groupMode === groupModes.groupByReadStrandMode && this.state.viewAsPairs) {
                    this.state.viewAsPairs = false;
                    this.viewAsPairsFlagChanged();
                }
                this.groupingModeChanged();
            }
            else if (viewAsPairsFlagChanged) {
                this.viewAsPairsFlagChanged();
            }
            if (alignments !== this.state.alignments) {
                this.alignmentsVisibilityChanged();
            }
            this._flags.renderFeaturesChanged = true;
            this.requestRenderRefresh();
            if (shouldReportTracksState) {
                this.reportTrackState();
            }
        };

        const wrapPerformFn = (fn) => () => {
            fn(this.bamRenderSettings);
            this.sortingModeChanged();
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
                            case key === 'perform':
                                result[key] = wrapPerformFn(menuEntry[key], menuEntry.name);
                                break;
                            default:
                                result[key] = wrapMutatorFn(menuEntry[key], menuEntry.name);
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
                if (event === 'bam>showAlignments') {
                    return;
                }
                const path = event.split('>');
                if (path && path[0] === 'bam') {
                    const menuItem = menuUtilities.findMenuItem(this._menu, event);
                    if (menuItem) {
                        if (menuItem.type === 'button') {
                            menuItem.perform();
                        }
                        else if (menuItem.type === 'checkbox' && menuItem.name !== 'bam>showAlignments') {
                            menuItem.isEnabled() ? menuItem.disable() : menuItem.enable();
                        }
                    }
                }
            }
        };

        this.showAlignmentsListener = (params) => {
            const menuItem = menuUtilities.findMenuItem(this._menu, params.event);
            if (menuItem && !params.disableShowAlignmentsForAllTracks) {
                menuItem.isEnabled() ? menuItem.disable() : menuItem.enable();
            } else {
                menuItem.disable();
            }
        };

        const _hotKeyListener = ::this.hotKeyListener;
        const _showAlignmentsListener = ::this.showAlignmentsListener;

        const self = this;
        this._removeListener = function () {
            self.dispatcher.removeListener('hotkeyPressed', _hotKeyListener);
            self.dispatcher.removeListener('bam:showAlignments', _showAlignmentsListener);
        };
        this.dispatcher.on('hotkeyPressed', _hotKeyListener);
        this.dispatcher.on('bam:showAlignments', _showAlignmentsListener);

        return this._menu;
    }

    constructor(opts) {
        super(opts);
        this.state.readsViewMode = parseInt(this.state.readsViewMode);
        this.cacheService = new BamCacheService(this, this.trackConfig);
        this._bamRenderer = new BamRenderer(this.viewport, Object.assign({}, this.trackConfig, this.config), this._pixiRenderer, this.cacheService, opts);

        const bamSettings = {
            chromosomeId: this.config.chromosomeId,
            id: this.config.openByUrl ? undefined : this.config.id,
            file: this.config.openByUrl ? this.config.bioDataItemId : undefined,
            index: this.config.openByUrl ? this.config.indexPath : undefined
        };
        const bamRenderSettings = Object.assign({
            filterFailedVendorChecks: true,
            filterPcrOpticalDuplicates: true,
            filterSecondaryAlignments: false,
            filterSupplementaryAlignments: false,
            isSoftClipping: false,
            maxBpCount: 10000,
            minBpCount: 50,
            sortMode: sortTypes.sortByStartLocation
        }, this.bamRenderSettings);

        bamRenderSettings.viewAsPairs = this.state.viewAsPairs;

        if (opts) {
            this.shouldDisplayAlignmentsCoverageTooltips = opts.displayAlignmentsCoverageTooltips;
            if (opts.isDownSampling) {
                bamSettings.count = opts.maxReadsCount;
                bamSettings.frame = opts.maxFrameSize;
                this.downsampled = true;
            }
            else {
                this.downsampled = false;
            }
            bamRenderSettings.isSoftClipping = opts.showSoftClippedBase;
            bamRenderSettings.filterFailedVendorChecks = opts.filterReads.failedVendorChecks;
            bamRenderSettings.filterPcrOpticalDuplicates = opts.filterReads.pcrOpticalDuplicates;
            bamRenderSettings.filterSecondaryAlignments = opts.filterReads.secondaryAlignments;
            bamRenderSettings.filterSupplementaryAlignments = opts.filterReads.supplementaryAlignments;
            bamRenderSettings.maxBpCount = opts.maxBpCount;
            bamRenderSettings.minBpCount = opts.minBpCount;
            this.shouldDisplayCenterLine = opts.showCenterLine;
            this.state = Object.assign(this.state, {softClip: bamRenderSettings.isSoftClipping});
            if (opts.dispatcher) {
                this.dispatcher = opts.dispatcher;
            }
        }

        this.bamRequestSettings = bamSettings;
        this.bamRenderSettings = bamRenderSettings;

        this.cacheService.properties = {
            maxAlignmentsRange: this._bamRenderer.maximumAlignmentsRange,
            maxCoverageRange: this._bamRenderer.maximumCoverageRange,
            rendering: bamRenderSettings,
            request: this.bamRequestSettings
        };

        this.cacheService.cache.groupMode = this.state.groupMode;
    }

    globalSettingsChanged(state) {
        super.globalSettingsChanged(state);
        let shouldReloadData = this._bamRenderer.globalSettingsChanged(state);
        const bamSettings = {
            chromosomeId: this.config.chromosomeId,
            id: this.config.openByUrl ? undefined : this.config.id,
            file: this.config.openByUrl ? this.config.bioDataItemId : undefined,
            index: this.config.openByUrl ? this.config.indexPath : undefined
        };
        const maxBpCount = 10000;
        const minBpCount = 50;
        const bamRenderSettings = Object.assign({
            filterFailedVendorChecks: true,
            filterPcrOpticalDuplicates: true,
            filterSecondaryAlignments: false,
            filterSupplementaryAlignments: false,
            isSoftClipping: false,
            maxBpCount,
            minBpCount,
            sortMode: sortTypes.sortByStartLocation
        }, this.bamRenderSettings);

        bamRenderSettings.viewAsPairs = this.state.viewAsPairs;

        this.shouldDisplayAlignmentsCoverageTooltips = state.displayAlignmentsCoverageTooltips;
        if (state.isDownSampling) {
            shouldReloadData = shouldReloadData || bamSettings.count !== state.maxReadsCount ||
                bamSettings.frame !== state.maxFrameSize || !this.downsampled;
            bamSettings.count = state.maxReadsCount;
            bamSettings.frame = state.maxFrameSize;
            this.downsampled = true;
        }
        else {
            shouldReloadData = shouldReloadData || this.downsampled;
            this.downsampled = false;
        }
        shouldReloadData = shouldReloadData ||
            bamRenderSettings.isSoftClipping !== state.showSoftClippedBase ||
            bamRenderSettings.filterFailedVendorChecks !== state.filterReads.failedVendorChecks ||
            bamRenderSettings.filterPcrOpticalDuplicates !== state.filterReads.pcrOpticalDuplicates ||
            bamRenderSettings.filterSecondaryAlignments !== state.filterReads.secondaryAlignments ||
            bamRenderSettings.filterSupplementaryAlignments !== state.filterReads.supplementaryAlignments ||
            bamRenderSettings.maxBpCount !== state.maxBpCount ||
            bamRenderSettings.minBpCount !== state.minBpCount;
        bamRenderSettings.isSoftClipping = state.showSoftClippedBase;
        bamRenderSettings.filterFailedVendorChecks = state.filterReads.failedVendorChecks;
        bamRenderSettings.filterPcrOpticalDuplicates = state.filterReads.pcrOpticalDuplicates;
        bamRenderSettings.filterSecondaryAlignments = state.filterReads.secondaryAlignments;
        bamRenderSettings.filterSupplementaryAlignments = state.filterReads.supplementaryAlignments;
        bamRenderSettings.maxBpCount = state.maxBpCount;
        bamRenderSettings.minBpCount = state.minBpCount;

        this.shouldDisplayCenterLine = state.showCenterLine;

        this.state = Object.assign(this.state, {softClip: bamRenderSettings.isSoftClipping});
        this.bamRequestSettings = bamSettings;
        this.bamRenderSettings = bamRenderSettings;

        if (shouldReloadData) {
            this.cacheService.properties = {
                maxAlignmentsRange: this._bamRenderer.maximumAlignmentsRange,
                maxCoverageRange: this._bamRenderer.maximumCoverageRange,
                rendering: bamRenderSettings,
                request: this.bamRequestSettings
            };
        }

        Promise.resolve().then(async() => {
            if (shouldReloadData) {
                await this.updateCache();
            }
            this._flags.renderFeaturesChanged = true;
            this.requestRenderRefresh();
        });
    }

    sortingModeChanged() {
        this.cacheService.cache.sort(this.bamRenderSettings.sortMode, Math.round(this.viewport.centerPosition));
        this._flags.renderFeaturesChanged = true;
        this.requestRenderRefresh();
    }

    viewAsPairsFlagChanged() {
        if (this.state.alignments) {
            if (this.state.viewAsPairs) {
                this.cacheService.cache.rearrangeAsPairReads();
            }
            else {
                this.cacheService.cache.rearrangeReads();
            }
        }
        this.bamRenderSettings.viewAsPairs = this.state.viewAsPairs;
    }

    groupingModeChanged() {
        this.cacheService.cache.groupMode = this.state.groupMode;
    }

    alignmentsVisibilityChanged() {
        if (this.state.alignments) {
            this.invalidateCache();
            Promise.resolve().then(async() => {
                await this.updateCache();
                this._flags.renderFeaturesChanged = true;
                this.requestRenderRefresh();
            });
        }
    }

    invalidateCache() {
        super.invalidateCache();
        if (this.cacheService && this.cacheService.cache) {
            this.cacheService.cache.invalidate();
        }
    }

    async updateCache() {
        if (this.cacheService.cache.isUpdating) {
            return false;
        }
        if (this.trackDataLoadingStatusChanged) {
            this.trackDataLoadingStatusChanged(true);
        }
        const currentMode = this.cacheService.cache.dataMode;
        let _dataMode = dataModes.full;
        if (!this.state.alignments) {
            _dataMode = dataModes.coverage;
        }
        if (this.viewport.actualBrushSize > this._bamRenderer.maximumCoverageRange) {
            _dataMode = dataModes.regions;
        } else if (this.viewport.actualBrushSize > this._bamRenderer.maximumAlignmentsRange) {
            _dataMode = dataModes.coverage;
        }
        const modeChanged = currentMode !== _dataMode;
        if (modeChanged) {
            this.cacheService.cache.dataMode = _dataMode;
        }
        const result = await this.cacheService.completeCacheData(this.viewport, this.state, {
            onStart: () => {
                this._flags.renderFeaturesChanged = true;
                this.requestRenderRefresh();
            }
        });
        if (!result && this.cacheService.cache.rangeIsEmpty(this.viewport)) {
            this._flags.renderFeaturesChanged = true;
            this.requestRenderRefresh();
        }
        if (this.trackDataLoadingStatusChanged) {
            this.trackDataLoadingStatusChanged(false);
        }
        return result;
    }

    async getNewCache() {
        return await this.updateCache();
    }

    canScroll(newPosition) {
        return this._bamRenderer.canSetYPosition(newPosition);
    }

    setScrollPosition(value) {
        this._bamRenderer.setYPositionFromCanvasPosition(value);
        this.requestRenderRefresh();
    }

    isScrollable() {
        if (this.viewport.actualBrushSize > this._bamRenderer.maximumAlignmentsRange) {
            return false;
        }
        return this._bamRenderer.isScrollable;
    }

    scrollIndicatorBoundaries() {
        return this._bamRenderer.scrollBarFrame;
    }

    onScroll({delta}) {
        this.tooltip.hide();
        super.onScroll({delta});
        this._bamRenderer.setScrollPosition({delta});
    }

    onClick({x, y}) {
        const hoveredItem = this._bamRenderer.checkFeature({x, y});
        if (hoveredItem && hoveredItem.type === HOVERED_ITEM_TYPE_REGION) {
            this.moveBrush({start: hoveredItem.item.startIndex, end: hoveredItem.item.endIndex});
        } else if (hoveredItem && hoveredItem.type === HOVERED_ITEM_TYPE_ALIGNMENT &&
            this.dataItemClicked !== null && this.dataItemClicked !== undefined &&
            hoveredItem.item && hoveredItem.item.render && hoveredItem.item.render.info) {
            this.tooltip.hide();
            this.dataItemClicked(this, {
                chromosome: this.config.chromosome,
                info: hoveredItem.item.render.info,
                read: hoveredItem.item
            }, {name: 'read-click-event', position: {x, y}});
        }
    }

    onDrag(opts) {
        if (this.viewport.actualBrushSize < this._bamRenderer.maximumRange) {
            super.onDrag(opts);
        }
    }

    _getTooltipContent(hoveredItem) {
        if (hoveredItem.type !== HOVERED_ITEM_TYPE_COVERAGE && !this.shouldDisplayTooltips) {
            return null;
        }
        switch (hoveredItem.type) {
            case HOVERED_ITEM_TYPE_ALIGNMENT: {
                if (hoveredItem.item.render) {
                    return hoveredItem.item.render.info;
                }
            }
                break;
            case HOVERED_ITEM_TYPE_COVERAGE: {
                if (this.shouldDisplayTooltips || this.shouldDisplayAlignmentsCoverageTooltips) {
                    return CoverageTransformer.getTooltipContent(hoveredItem.item.dataItem, this.cacheService.cache.dataMode === dataModes.coverage);
                }
            }
                break;
            case HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR: {
                return [
                    ['Interval:', `${hoveredItem.item.startIndex}-${hoveredItem.item.endIndex}`],
                    [`${hoveredItem.item.value} reads removed.`]
                ];
            }
            case HOVERED_ITEM_TYPE_SPLICE_JUNCTION: {
                return [
                    ['Start:', `${hoveredItem.item.start}`],
                    ['End:', `${hoveredItem.item.end}`],
                    ['Depth:', `${hoveredItem.item.count}`],
                    ['Strand:', `${hoveredItem.item.strand ? 'positive' : 'negative'}`],
                ];
            }
        }
        return null;
    }

    onMouseOut() {
        super.onMouseOut();
        if (this._bamRenderer && this._bamRenderer.hoverFeature(null)) {
            this._flags.hoverChanged = true;
            this.requestRenderRefresh();
        }
    }

    onHover({x, y}) {
        if (super.onHover({x, y})) {
            const hoveredItem = this._bamRenderer.checkFeature({x, y});
            if (this.hoveringEffects) this._bamRenderer.hoverFeature(hoveredItem);
            if (!hoveredItem) {
                this.tooltip.hide();
                return true;
            }
            const tooltipContent = this._getTooltipContent(hoveredItem);
            if (!tooltipContent) {
                this.tooltip.hide();
                return true;
            } else {
                this.tooltip.setContent(tooltipContent);
                this.tooltip.move({x, y});
                this.tooltip.show({x, y});
                return false;
            }
        } else {
            this._bamRenderer.hoverFeature(null);
        }
        return false;
    }

    render(flags) {
        if (flags.renderReset) {
            this.container.removeChildren();
            this.container.addChild(this._bamRenderer.container);
        }
        this._bamRenderer.height = this.height;
        return this._bamRenderer.render(flags, this.state);
    }

    hoverVerticalScroll() {
        this._bamRenderer.hoverScrollBar();
        this.updateScene();
    }

    unhoverVerticalScroll() {
        this._bamRenderer.unhoverScrollBar();
        this.updateScene();
    }

    clearData() {
        super.clearData();
        this.cacheService.clear();
        this.cacheService = null;
    }

    destructor() {
        super.destructor();
        if (this._removeListener) {
            this._removeListener();
        }
    }
}