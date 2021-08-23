import {
    BamCacheService,
    BamRenderer,
    CoverageTransformer,
    HOVERED_ITEM_TYPE_ALIGNMENT,
    HOVERED_ITEM_TYPE_COVERAGE,
    HOVERED_ITEM_TYPE_DOWNSAMPLE_INDICATOR,
    HOVERED_ITEM_TYPE_REGION,
    HOVERED_ITEM_TYPE_SPLICE_JUNCTION
} from './internal';
import {default as bamMenu, sashimiMenu} from './menu';
import {dataModes, groupModes, sortTypes} from './modes';
import BAMConfig from './bamConfig';
import Promise from 'bluebird';
import {ScrollableTrack} from '../../core';
import {menu as menuUtilities} from '../../utilities';
import Menu from '../../core/menu';
import {scaleModes} from '../wig/modes';

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

    static preStateMutatorFn = (track) => {
        const viewAsPairsFlag = track.state.viewAsPairs;
        const groupingMode = track.state.groupMode;
        const currentDisplayMode = track.state.coverageDisplayMode;
        const currentScaleMode = track.state.coverageScaleMode;
        const logScaleEnabled = track.state.coverageLogScale;
        const alignments = track.state.alignments;
        return {
            alignments,
            currentDisplayMode,
            currentScaleMode,
            groupingMode,
            logScaleEnabled,
            viewAsPairsFlag
        };
    };

    static postStateMutatorFn = (track, key, prePayload) => {
        let shouldReportTracksState = true;
        const {
            alignments,
            currentDisplayMode,
            currentScaleMode,
            groupingMode,
            logScaleEnabled,
            viewAsPairsFlag
        } = prePayload;
        if ((!track.state.alignments || track.state.alignments !== alignments) && track.changeTrackHeight) {
            if (track.state.alignments) {
                track.changeTrackHeight(track.trackConfig.defaultHeight);
            } else {
                let newHeight = 0;
                if (track.state.coverage) {
                    newHeight += track.trackConfig.coverage.height;
                }
                if (track.state.spliceJunctions) {
                    newHeight += track.trackConfig.spliceJunctions.height;
                }
                track.changeTrackHeight(newHeight);
            }
        }
        if (key === 'coverage>scale>manual') {
            shouldReportTracksState = false;
        } else if (key === 'coverage>scale>group-auto-scale') {
            shouldReportTracksState = true;
            if (track.cacheService) {
                track.cacheService.transform(track.viewport, track.state);
            }
        } else if (currentScaleMode !== track.state.coverageScaleMode) {
            track._flags.dataChanged = true;
            track.state.coverageScaleFrom = undefined;
            track.state.coverageScaleTo = undefined;
            shouldReportTracksState = currentScaleMode === scaleModes.groupAutoScaleMode;
            track._flags.dataChanged = true;
        } else if (logScaleEnabled !== track.state.coverageLogScale) {
            track._flags.dataChanged = true;
        } else if (currentDisplayMode !== track.state.coverageDisplayMode) {
            track._flags.dataChanged = true;
        }
        const viewAsPairsFlagChanged = viewAsPairsFlag !== track.state.viewAsPairs;
        if (viewAsPairsFlagChanged) {
            if (track.state.groupMode === groupModes.groupByReadStrandMode && track.state.viewAsPairs) {
                track.state.groupMode = groupModes.defaultGroupingMode;
            }
            track.bamRenderSettings.viewAsPairs = track.state.viewAsPairs;
        }
        const groupingModeChanged = groupingMode !== track.state.groupMode;
        if (groupingModeChanged) {
            if (track.state.groupMode === groupModes.groupByReadStrandMode && track.state.viewAsPairs) {
                track.state.viewAsPairs = false;
                track.viewAsPairsFlagChanged();
            }
            track.groupingModeChanged();
        }
        else if (viewAsPairsFlagChanged) {
            track.viewAsPairsFlagChanged();
        }
        if (alignments !== track.state.alignments) {
            track.alignmentsVisibilityChanged();
        }
        track._flags.renderFeaturesChanged = true;
        track.requestRenderRefresh();
        if (shouldReportTracksState) {
            track.reportTrackState();
        }
    };

    static postPerformFn = (track) => {
        track.sortingModeChanged();
        track.reportTrackState();
    };

    static afterStateMutatorFn = (tracks, key) => {
        if (key === 'coverage>scale>manual') {
            const getCoverageExtremum = (track) => {
                let max = track.state.coverageScaleTo;
                let min = track.state.coverageScaleFrom;
                const hasRealValues = track.cacheService &&
                    track.cacheService.cache &&
                    track.cacheService.cache.coverage &&
                    track.cacheService.cache.coverage.coordinateSystem;
                const isNone = o => o === undefined || o === null;
                if (isNone(max) && hasRealValues) {
                    max = track.cacheService.cache.coverage.coordinateSystem.realMaximum;
                }
                if (isNone(min) && hasRealValues) {
                    min = track.cacheService.cache.coverage.coordinateSystem.realMinimum;
                }
                return {max, min};
            };
            const getCoverageExtremums = () => {
                const values = (tracks || []).map(getCoverageExtremum);
                return values.reduce((r, c) => ({
                    max: Math.min(c.max, r.max),
                    min: Math.max(c.min, r.min)
                }), {max: Infinity, min: -Infinity});
            };
            const isLogScale = (tracks || [])
                .map(track => track.state.coverageLogScale)
                .reduce((r, c) => r && c, true);
            const [dispatcher] = (tracks || [])
                .map(track => track.config.dispatcher)
                .filter(Boolean);
            const [browserId] = (tracks || [])
                .map(track => track.config.browserId)
                .filter(Boolean);
            if (dispatcher) {
                dispatcher.emitSimpleEvent('tracks:coverage:manual:configure', {
                    config: {
                        extremumFn: getCoverageExtremums,
                        isLogScale
                    },
                    options: {
                        browserId,
                        group: (tracks || []).length > 1
                    },
                    sources: (tracks || []).map(track => track.config.name)
                });
            }
        }
    }

    static Menu = Menu(
        bamMenu,
        {
            postPerformFn: BAMTrack.postPerformFn,
            postStateMutatorFn: BAMTrack.postStateMutatorFn,
            preStateMutatorFn: BAMTrack.preStateMutatorFn,
            afterStateMutatorFn: BAMTrack.afterStateMutatorFn,
        }
    );

    static SashimiMenu = Menu(
        sashimiMenu,
        {}
    );

    get stateKeys() {
        return [
            'arrows',
            'alignments',
            'colorMode',
            'coverage',
            'diffBase',
            'groupMode',
            'header',
            'ins_del',
            'mismatches',
            'readsViewMode',
            'shadeByQuality',
            'softClip',
            'spliceJunctions',
            'viewAsPairs',
            'coverageDisplayMode',
            'coverageLogScale',
            'coverageScaleMode',
            'coverageScaleFrom',
            'coverageScaleTo',
            'sashimi',
            'wigColors',
            'groupAutoScale'
        ];
    }

    get trackHasCoverageSubTrack() {
        return true;
    }

    get trackIsResizable() {
        if (this.state) {
            return this.state.alignments || this.state.sashimi;
        }
        return true;
    }

    getSettings() {
        if (this._menu) {
            return this._menu;
        }
        this._menu = this.state.sashimi
            ? this.constructor.SashimiMenu.attach(this, {browserId: this.browserId})
            : this.constructor.Menu.attach(this, {browserId: this.browserId});

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
        const cacheServiceInitialized = !!opts.cacheService;
        this.state.readsViewMode = parseInt(this.state.readsViewMode);
        this.state.spliceJunctionsFiltering = opts.spliceJunctionsFiltering
            ? opts.spliceJunctionsCoverageThreshold
            : 0;
        this.cacheService = opts.cacheService || new BamCacheService(this, Object.assign({}, this.trackConfig, this.config));
        this._bamRenderer = new BamRenderer(
            this.viewport,
            Object.assign({}, this.trackConfig, this.config),
            this.cacheService,
            opts,
            this
        );

        const bamSettings = {
            chromosomeId: this.config.chromosomeId,
            file: this.config.openByUrl ? this.config.bioDataItemId : undefined,
            id: this.config.openByUrl ? undefined : this.config.id,
            index: this.config.openByUrl ? this.config.indexPath : undefined,
            projectId: this.config.project ? this.config.project.id : undefined,
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

        if (!cacheServiceInitialized) {
            this.cacheService.properties = {
                maxAlignmentsRange: this._bamRenderer.maximumAlignmentsRange,
                maxCoverageRange: this._bamRenderer.maximumCoverageRange,
                rendering: bamRenderSettings,
                request: this.bamRequestSettings
            };
        }
        this.cacheService.cache.groupMode = this.state.groupMode;

        this._actions = [
            {
                enabled: () => (this.state.spliceJunctions || this.state.sashimi) &&
                    this.state.spliceJunctionsFiltering > 0,
                label: 'Splice junctions minimum coverage: ',
                type: 'text',
                value: () => this.state.spliceJunctionsFiltering,
            }
        ];
    }

    trackSettingsChanged(params) {
        if (this.config.bioDataItemId === params.id) {
            const settings = params.settings;
            settings.forEach(setting => {
                const menuItem = menuUtilities.findMenuItem(this._menu, setting.name);
                if (menuItem.type === 'checkbox') {
                    if (setting.name === 'coverage>scale>manual') {
                        if (setting.value) {
                            this.state.coverageScaleFrom = setting.extraOptions.from;
                            this.state.coverageScaleTo = setting.extraOptions.to;
                            this.state.coverageScaleMode = scaleModes.manualScaleMode;
                        } else {
                            this.state.coverageScaleMode = scaleModes.defaultScaleMode;
                        }
                        this._flags.dataChanged = true;
                    } else if (setting.name.indexOf('bam>readsView') !== -1 && setting.name !== 'bam>readsView>pairs') {
                        menuItem.enable();
                    } else {
                        setting.value ? menuItem.enable() : menuItem.disable();
                    }
                } else if (menuItem.type === 'button') {
                    menuItem.perform();
                }
            });
        }
    }

    globalSettingsChanged(state) {
        super.globalSettingsChanged(state);
        let shouldReloadData = this._bamRenderer.globalSettingsChanged(state)
            || this.cacheService._coverageTransformer.alleleFrequencyThresholdBam !== state.alleleFrequencyThresholdBam;
        this.cacheService._coverageTransformer.alleleFrequencyThresholdBam = state.alleleFrequencyThresholdBam;

        const bamSettings = {
            chromosomeId: this.config.chromosomeId,
            file: this.config.openByUrl ? this.config.bioDataItemId : undefined,
            id: this.config.openByUrl ? undefined : this.config.id,
            index: this.config.openByUrl ? this.config.indexPath : undefined,
            projectId: this.config.project ? this.config.project.id : undefined
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

        this.state = Object.assign(
            this.state,
            {
                softClip: bamRenderSettings.isSoftClipping,
                spliceJunctionsFiltering: state.spliceJunctionsFiltering
                    ? state.spliceJunctionsCoverageThreshold
                    : 0
            }
        );
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

        Promise.resolve().then(async () => {
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
            Promise.resolve().then(async () => {
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
        if (!this.state.alignments && !this.state.sashimi) {
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
            this.moveBrush({end: hoveredItem.item.endIndex, start: hoveredItem.item.startIndex});
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
