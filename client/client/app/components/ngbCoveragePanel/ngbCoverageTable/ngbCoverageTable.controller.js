const COVERAGE_TABLE_COLUMNS = ['chr', 'start', 'end', 'coverage'];
const ROW_HEIGHT = 30;

export default class ngbCoverageTableController {

    loadingData = false;

    get coverageTableColumns() {
        return COVERAGE_TABLE_COLUMNS;
    }
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

    constructor($scope, $timeout, projectContext, ngbCoveragePanelService) {
        Object.assign(this, {$scope, $timeout, projectContext, ngbCoveragePanelService});
        this.gridOptions.rowHeight = this.rowHeight;
        this.gridOptions.infiniteScrollDown = this.totalCount > this.pageSize;
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
    get currentCoverageIndex() {
        return this.ngbCoveragePanelService.currentCoverageIndex;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.getMotifsResultsGridColumns(),
            data: this.ngbCoveragePanelService.coverageSearchResults,
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
        const headerCells = require('./ngbCoverageTable_header.tpl.html');

        const result = [];
        const columnsList = this.coverageTableColumns;
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

    rowClick(row) {
        const chromosome = row.entity.chr.replace(/chr/, '');
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
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageIndex, false);
        this.loadData(request, false);
    }

    async getDataUp() {
        if (this.currentPages.first <= 1) {
            return;
        }
        this.loadingData = true;
        this.$scope.$apply();
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(this.currentCoverageIndex, true);
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
            this.gridOptions.columnDefs = this.getMotifsResultsGridColumns();
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
}
