import baseController from '../../../shared/baseController';

const MOTIFS_RESULTS_COLUMNS = ['chromosome', 'start', 'end', 'strand', 'gene'];

export default class ngbMotifsResultsTableController  extends baseController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 20,
        rowHeight: 20,
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: false,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        treeRowHeaderAlwaysVisible: false,
        enableInfiniteScroll: true,
        infiniteScrollDown: true,
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: false,
        saveWidths: true,
        saveOrder: false,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: false,
        saveFilter: false,
        savePinning: false,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false
    };
    get motifsResultsColumns() {
        return MOTIFS_RESULTS_COLUMNS;
    }
    loadingData = false;

    get positive () {
        return this.ngbMotifsPanelService.positive;
    }
    get negative () {
        return this.ngbMotifsPanelService.negative;
    }
    get rowHeight () {
        this.ngbMotifsPanelService.rowHeight;
    }
    get pageSize () {
        return this.ngbMotifsPanelService.pageSize;
    }
    get searchStopOn () {
        return this.ngbMotifsPanelService.searchStopOn;
    }
    get motifsResultsTitle () {
        return this.ngbMotifsPanelService.motifsResultsTitle;
    }
    get currentParams () {
        return this.ngbMotifsPanelService.currentParams;
    }
    get searchRequestsHistory () {
        return this.ngbMotifsPanelService.searchRequestsHistory;
    }
    get emptyResults () {
        return !this.loading &&
            !this.ngbMotifsPanelService.isShowParamsTable && (
            !this.ngbMotifsPanelService.searchMotifResults ||
            this.ngbMotifsPanelService.searchMotifResults.length === 0
        );
    }

    static get UID() {
        return 'ngbMotifsResultsTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        motifsContext,
        projectContext,
        ngbMotifsPanelService
    ) {
        super();
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            motifsContext,
            projectContext,
            ngbMotifsPanelService
        });
        this.gridOptions.rowHeight = this.rowHeight;
        const showResults = this.showResults.bind(this);
        const addTracks = this.addTracks.bind(this);
        this.dispatcher.on('motifs:show:results', showResults);
        this.dispatcher.on('motifs:add:tracks', addTracks);
        this.$scope.$on('$destroy', () => {
            this.dispatcher.removeListener('motifs:show:results', showResults);
            this.dispatcher.removeListener('motifs:add:tracks', addTracks);
        });
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.getMotifsResultsGridColumns(),
            data: this.ngbMotifsPanelService.searchMotifResults,
            appScopeProvider: this.$scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, ::this.getDataDown);
                this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, ::this.getDataUp);
            }
        });
    }

    getMotifsResultsGridColumns() {
        const headerCells = require('../ngbMotifsTable_header.tpl.html');

        const result = [];
        const columnsList = this.motifsResultsColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                enableHiding: false,
                enableFiltering: false,
                enableSorting: false,
                field: column,
                headerCellTemplate: headerCells,
                headerTooltip: column,
                minWidth: 40,
                displayName: column,
                width: '*'
            };
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    rowClick (row) {
        this.addTracks(row.entity);
    }

    async getDataDown () {
        const {startPosition, chromosomeId} = this.searchStopOn;
        if (startPosition === null || chromosomeId === null) {
            return;
        }
        const request = {
            chromosomeId,
            startPosition,
            ...this.currentParams
        };
        delete request.name;
        await this.loadData(request, false);
        this.searchRequestsHistory.push(request);
        this.gridOptions.infiniteScrollUp = true;
    }

    async getDataUp () {
        if (!this.searchRequestsHistory.length) {
            return;
        }
        this.searchRequestsHistory.pop();
        const index = this.searchRequestsHistory.length - 2;
        const {startPosition, chromosomeId} = this.searchRequestsHistory[index];
        const request = {
            chromosomeId,
            startPosition,
            ...this.currentParams
        };
        delete request.name;
        await this.loadData(request, true);
    }

    async loadData (request, isScrollTop) {
        this.loadingData = true;
        this.$timeout(() => this.$scope.$apply());
        const results = await this.ngbMotifsPanelService.getSearchMotifsResults(request)
            .then(success => {
                if (success) {
                    return this.ngbMotifsPanelService.searchMotifResults;
                }
                return [];
            });
        this.loadingData = false;
        if (results) {
            this.gridOptions.columnDefs = this.getMotifsResultsGridColumns();
            const data = isScrollTop ?
                results.concat(this.gridOptions.data) :
                this.gridOptions.data.concat(results);
            this.gridOptions.data = data;
            if (isScrollTop !== undefined && this.gridApi) {
                const {startPosition, chromosomeId} = this.searchStopOn;
                const state = this.gridApi.saveState.save();
                return this.gridApi.infiniteScroll.dataLoaded(
                    this.searchRequestsHistory.length > 2,
                    startPosition !== null && chromosomeId !== null)
                    .then(() => {
                        const maxDataLength = this.pageSize * 2;
                        if (data.length > maxDataLength) {
                            this.gridApi.infiniteScroll.saveScrollPercentage();
                            if (isScrollTop) {
                                this.gridOptions.data = this.gridOptions.data.slice(0, maxDataLength);
                                const lastElementOnPage = () => {
                                    const windowHeight = window.innerHeight;
                                    const rowHeight = this.rowHeight;
                                    const last = this.pageSize + Math.floor(windowHeight/rowHeight);
                                    return last;
                                };
                                this.$timeout(() => {
                                    this.gridApi.core.scrollTo(
                                        this.gridOptions.data[lastElementOnPage()],
                                        this.gridOptions.columnDefs[0]
                                    );
                                });
                            } else {
                                this.gridOptions.data = this.gridOptions.data.slice(-maxDataLength);
                                this.$timeout(() => {
                                    this.gridApi.infiniteScroll.dataRemovedTop(
                                        this.searchRequestsHistory.length > 1,
                                        startPosition !== null && chromosomeId !== null
                                    );
                                });
                            }
                        }
                        this.gridApi.saveState.restore(this.$scope, state);
                    });
            }
            this.$timeout(() => this.$scope.$apply());
        }
    }

    showResults() {
        this.gridOptions.data = this.ngbMotifsPanelService.searchMotifResults;
        this.$timeout(() => this.$scope.$apply());
    }

    backToParamsTable () {
        this.ngbMotifsPanelService.backToParamsTable();
    }

    async addTracks (row) {
        const {
            chromosome,
            start,
            end,
        } = row;
        const {
            referenceId,
            name,
            motif,
            searchType
        } = this.currentParams;
        const currentMatch = {
            chromosome,
            start,
            end,
            referenceId,
            motif,
            searchType
        };

        const range = Math.abs(end - start);
        const rangeStart = Math.min(start, end) - range;
        const rangeEnd = Math.max(start, end) + range;

        const strand = this.ngbMotifsPanelService.getStrand(row.strand).toLowerCase();
        const getName = strand => `${name || motif}_${strand}`;
        const reference = this.projectContext.reference;

        const tracksOptions = {};
        const motifsTracks = (this.projectContext.tracks || [])
            .filter(track => track.format === 'MOTIFS');
        const trackId = this.ngbMotifsPanelService.requestNumber;

        const motifsTrackPattern = strand => ({
            name: getName(strand),
            format: 'MOTIFS',
            isLocal: true,
            projectId: '',
            bioDataItemId: getName(strand),
            id: strand === this.positive ? trackId : trackId + 1,
            reference,
            referenceId
        });
        const motifsTrackStatePattern = strand => ({
            bioDataItemId: getName(strand),
            duplicateId: undefined,
            projectId: '',
            isLocal: true,
            format: 'MOTIFS',
            state: {
                motifStrand: strand,
                motif
            }
        });

        const referenceTrackState = {
            referenceShowForwardStrand: true,
            referenceShowReverseStrand: true,
            referenceShowTranslation: false
        };
        tracksOptions.tracks = (this.projectContext.tracks || []);
        tracksOptions.tracksState = (this.projectContext.tracksState || []);
        const [existingReferenceTrackState] = tracksOptions.tracksState
            .filter(track => track.format === 'REFERENCE');
        if (existingReferenceTrackState) {
            existingReferenceTrackState.state = {
                ...(existingReferenceTrackState.state || {}),
                ...referenceTrackState
            };
        }
        const referenceTrackStateIndex = tracksOptions
            .tracksState.indexOf(existingReferenceTrackState);

        const motifsTracksNames = (motifsTracks || []).map(track => track.name);
        let positiveTrackPosition = 0;
        let negativeTrackPosition = 0;

        if (
            !motifsTracksNames.includes(getName(this.positive)) &&
            !motifsTracksNames.includes(getName(this.negative))
        ) {
            positiveTrackPosition = 1;
            negativeTrackPosition = 2;
        } else {
            if (!motifsTracksNames.includes(getName(strand))) {
                positiveTrackPosition = strand === this.positive ? 1 : 0;
                negativeTrackPosition = strand === this.negative ? 1 : 0;
            }
        }
        if (positiveTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern(this.positive));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + positiveTrackPosition,
                0,
                motifsTrackStatePattern(this.positive)
            );
        }
        if (negativeTrackPosition !== 0) {
            tracksOptions.tracks.push(motifsTrackPattern(this.negative));
            tracksOptions.tracksState.splice(
                referenceTrackStateIndex + negativeTrackPosition,
                0,
                motifsTrackStatePattern(this.negative)
            );
        }
        const [currentReferenceTrack] = this.projectContext.getActiveTracks()
                .filter(track => track.format === 'REFERENCE');
        if (currentReferenceTrack && currentReferenceTrack.instance) {
            currentReferenceTrack.instance.state.referenceShowForwardStrand = true;
            currentReferenceTrack.instance.state.referenceShowReverseStrand = true;
            currentReferenceTrack.instance.reportTrackState(true);
            currentReferenceTrack.instance.requestRender();
        }
        this.projectContext.changeState({
            viewport: {
                start: rangeStart,
                end: rangeEnd
            },
            chromosome: {
                name: chromosome
            },
            ...tracksOptions
        }, false, () => {
            this.setMotif(currentMatch);
        });
    }

    setMotif (currentMatch) {
        this.motifsContext.setMotif(currentMatch);
    }
}
