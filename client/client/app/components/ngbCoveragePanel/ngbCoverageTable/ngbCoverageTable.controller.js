const ROW_HEIGHT = 30;

export default class ngbCoverageTableController {

    loadingData = false;

    get rowHeight() {
        return ROW_HEIGHT;
    }

    gridOptions = {
        height: '100%',
        headerRowHeight: 20,
        rowHeight: 30,
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enablePinning: false,
        enableHorizontalScrollbar: 0,
        treeRowHeaderAlwaysVisible: false,
        enableInfiniteScroll: true,
        infiniteScrollDown: false,
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: false,
        saveWidths: true,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: true,
        saveFilter: false,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false
    };

    static get UID() {
        return 'ngbCoverageTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        projectContext,
        ngbCoveragePanelService,
        ngbCoverageTableService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            ngbCoveragePanelService,
            ngbCoverageTableService
        });
        this.gridOptions.rowHeight = this.rowHeight;
        this.gridOptions.infiniteScrollDown = this.totalCount > this.pageSize;

        const restoreState = this.restoreState.bind(this);
        const panelChanged = this.panelChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('coverage:table:restore', restoreState);
        dispatcher.on('layout:active:panel:change', panelChanged);
        dispatcher.on('coverage:filter:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('coverage:table:restore', restoreState);
            dispatcher.removeListener('layout:active:panel:change', panelChanged);
            dispatcher.removeListener('coverage:filter:changed', filterChanged);
        });
    }

    get isLastPage() {
        return this.currentPages.last >= Math.ceil(this.totalCount / this.pageSize);
    }

    get emptyResults() {
        return this.ngbCoveragePanelService.emptyResults;
    }
    get pageSize() {
        return this.ngbCoveragePanelService.pageSize;
    }
    get totalCount() {
        return this.ngbCoveragePanelService.totalCount;
    }
    get currentPages() {
        return this.ngbCoveragePanelService.currentPages;
    }
    get currentCoverageId() {
        return this.ngbCoveragePanelService.currentCoverageIndex.coverageId;
    }
    get sortInfo() {
        return this.ngbCoveragePanelService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbCoveragePanelService.sortInfo = value;
    }
    get displayFilters() {
        return this.ngbCoveragePanelService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbCoveragePanelService.displayFilters = value;
    }
    get isFilteredSearchFailure() {
        return this.ngbCoveragePanelService.isFilteredSearchFailure;
    }
    get filteredErrorMessageList() {
        return this.ngbCoveragePanelService.filteredErrorMessageList;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.ngbCoverageTableService.getMotifsResultsGridColumns(),
            data: this.ngbCoveragePanelService.coverageSearchResults,
            appScopeProvider: this.$scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, ::this.getDataDown);
                this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, ::this.getDataUp);
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
    }

    rowClick(row) {
        const chromosome = row.entity.chr;
        const currentChromosome = this.projectContext.currentChromosome ?
            this.projectContext.currentChromosome.name : null;
        const start = row.entity.start;
        const end = row.entity.end;

        if (chromosome !== currentChromosome) {
            this.projectContext.changeState({
                chromosome: {
                    name: chromosome
                },
                viewport: {
                    end,
                    start
                }
            });
        } else {
            this.projectContext.changeState({
                viewport: {
                    start,
                    end
                }
            });
        }
    }

    async getDataDown() {
        if (this.isLastPage) {
            return;
        }
        this.loadingData = true;
        this.$scope.$apply();
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageId, false);
        this.loadData(request, false);
    }

    async getDataUp() {
        if (this.currentPages.first <= 1) {
            return;
        }
        this.loadingData = true;
        this.$scope.$apply();
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageId, true);
        this.loadData(request, true);
    }

    async loadData(request, isScrollTop) {
        const results = await this.ngbCoveragePanelService.searchBamCoverage(request)
            .then(success => {
                if (success) {
                    return this.ngbCoveragePanelService.coverageSearchResults;
                }
                return [];
            });
        this.loadingData = false;
        if (results) {
            this.gridOptions.columnDefs = this.ngbCoverageTableService.getMotifsResultsGridColumns();
            const data = isScrollTop ?
                results.concat(this.gridOptions.data) :
                this.gridOptions.data.concat(results);
            this.gridOptions.data = data;
            if (isScrollTop !== undefined && this.gridApi) {
                const state = this.gridApi.saveState.save();
                return this.gridApi.infiniteScroll.dataLoaded(
                    this.currentPages.first > 1,
                    !this.isLastPage)
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
                                        this.currentPages.first > 1,
                                        !this.isLastPage
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

    async sortChanged(grid, sortColumns) {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        if (sortColumns && sortColumns.length > 0) {
            this.sortInfo = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: sc.field
            }));
        } else {
            this.sortInfo = null;
        }
        const sortingConfiguration = sortColumns
            .filter(column => !!column.sort)
            .map((column, priority) => ({
                field: column.field,
                sort: ({
                    ...column.sort,
                    priority
                })
            }));
        const {columns = []} = grid || {};
        columns.forEach(columnDef => {
            const [sortingConfig] = sortingConfiguration
                .filter(c => c.field === columnDef.field);
            if (sortingConfig) {
                columnDef.sort = sortingConfig.sort;
            }
        });
        this.ngbCoveragePanelService.resetCurrentPages();
        this.gridOptions.data = [];
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageId, false);
        this.loadData(request);
    }

    async restoreState() {
        this.loadingData = true;
        this.ngbCoveragePanelService.resetCurrentPages();
        this.ngbCoveragePanelService.sortInfo = null;
        this.ngbCoveragePanelService.displayFilters = false;
        this.ngbCoveragePanelService.clearFilters();
        this.gridOptions.data = [];
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageId, false);
        this.loadData(request);
    }

    panelChanged(panel) {
        if (this.gridApi && panel === 'ngbCoveragePanel') {
            this.gridApi.infiniteScroll.setScrollDirections(false, false);
            this.gridApi.infiniteScroll.saveScrollPercentage();
            this.gridApi.core.handleWindowResize();
            this.$timeout(() => {
                this.gridApi.infiniteScroll.dataLoaded(
                    this.currentPages.first > 1,
                    !this.isLastPage);
            });
        }
    }

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.ngbCoveragePanelService.resetCurrentPages();
        this.gridOptions.data = [];
        this.gridApi.infiniteScroll.setScrollDirections(false, false);
        this.gridApi.infiniteScroll.saveScrollPercentage();
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageId, false);
        await this.loadData(request);
        this.$timeout(() => {
            this.gridApi.infiniteScroll.dataLoaded(
                this.currentPages.first > 1,
                !this.isLastPage);
        });
    }
}
