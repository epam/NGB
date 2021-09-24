import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbMotifsTableController  extends baseController {

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
    _motifsSearchTitle = null;
    searchRequestsHistory = [];

    get rowHeight () {
        return ROW_HEIGHT;
    }

    static get UID() {
        return 'ngbMotifsTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        projectContext,
        ngbMotifsPanelService,
        ngbMotifsTableService
    ) {
        super();
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            ngbMotifsPanelService,
            ngbMotifsTableService
        });
        this.gridOptions.rowHeight = this.rowHeight;
        this.dispatcher.on('motifs:search:change', ::this.backToParamsTable);
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.ngbMotifsTableService.getMotifsGridColumns(),
            data: this.ngbMotifsPanelService.searchMotifsParams,
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

    get pageSize () {
        return this.ngbMotifsTableService.pageSize;
    }

    get isShowParamsTable () {
        return this.ngbMotifsPanelService.isShowParamsTable;
    }

    get searchStopOn () {
        return this.ngbMotifsPanelService.searchStopOn;
    }

    get motifsSearchTitle () {
        return this._motifsSearchTitle;
    }

    set motifsSearchTitle (row) {
        this._motifsSearchTitle = row.name ? row.name : row.motif;
    }

    rowClick(row) {
        if (this.isShowParamsTable) {
            this.showResultsTable(row.entity);
        } else {
            this.ngbMotifsTableService.addTracks(row.entity);
        }
    }

    async showResultsTable (row) {
        this.ngbMotifsPanelService.isShowParamsTable = false;
        this.motifsSearchTitle = row;
        this.ngbMotifsTableService.currentParams = row;
        const currentParams = this.ngbMotifsTableService.currentParams;
        const chromosomeType = this.ngbMotifsPanelService.chromosomeType;
        const chromosomeId = row.currentChromosomeId;
        const request = row['search type'] === chromosomeType ?
            {chromosomeId, ...currentParams} : {...currentParams};
        this.gridOptions.data = [];
        await this.loadData(request);
        this.searchRequestsHistory.push(request);
    }

    async getDataDown () {
        const {startPosition, chromosomeId} = this.searchStopOn;
        if (startPosition === null || chromosomeId === null) {
            return;
        }
        const currentParams = this.ngbMotifsTableService.currentParams;
        const request = {chromosomeId, startPosition, ...currentParams};
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
        const currentParams = this.ngbMotifsTableService.currentParams;
        const request = {chromosomeId, startPosition, ...currentParams};
        await this.loadData(request, true);
    }

    async loadData (request, isScrollTop) {
        const results = await this.ngbMotifsPanelService.searchMotifRequest(request)
            .then(success => {
                if (success) {
                    return this.ngbMotifsPanelService.searchMotifResults;
                }
            });
        if (results && results.length) {
            this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
            const data = isScrollTop ?
                results.concat(this.gridOptions.data) :
                this.gridOptions.data.concat(results);
            this.gridOptions.data = data;
            if (isScrollTop !== undefined && this.gridApi) {
                const {startPosition, chromosomeId} = this.searchStopOn;
                const state = this.gridApi.saveState.save();
                return this.gridApi.infiniteScroll.dataLoaded(
                    this.searchRequestsHistory.length > 1,
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
        } else {
            this.backToParamsTable();
            return this.$timeout(() => this.$scope.$apply());
        }
    }

    backToParamsTable() {
        this.ngbMotifsTableService.currentParams = {};
        this.motifsSearchTitle = '';
        this.searchRequestsHistory = [];
        this.ngbMotifsTableService.searchMotifResults = [];
        this.ngbMotifsPanelService.isShowParamsTable = true;
        this.gridOptions.data = this.ngbMotifsPanelService.searchMotifsParams;
        this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
    }
}
