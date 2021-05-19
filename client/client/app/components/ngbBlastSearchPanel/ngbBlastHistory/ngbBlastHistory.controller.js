import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbBlastHistoryController extends baseController {
    static get UID() {
        return 'ngbBlastHistoryController';
    }

    dispatcher;
    projectContext;

    isProgressShown = true;
    errorMessageList = [];
    historyLoadError = null;

    gridOptions = {
        enableSorting: true,
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        height: '100%',
        multiSelect: false,
        rowHeight: ROW_HEIGHT,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: true,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: false,
        saveFilter: false,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false,
    };

    constructor($scope, $timeout, $mdDialog, ngbBlastHistoryTableService, ngbBlastSearchService, uiGridConstants, dispatcher, projectContext) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            $mdDialog,
            dispatcher,
            ngbBlastHistoryTableService,
            ngbBlastSearchService,
            projectContext,
            uiGridConstants,
        });

        this.initEvents();
    }

    $onInit() {
        this.initialize();
    }

    // refreshScope(needRefresh) {
    //     if (needRefresh) {
    //         this.$scope.$apply();
    //     }
    // }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.historyLoadError = null;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbBlastHistoryTableService.getBlastHistoryGridColumns(),
            paginationPageSize: this.ngbBlastSearchService.historyPageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                // this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                // this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        await this.loadData();
    }

    async loadData() {
        try {
            await this.ngbBlastSearchService.updateSearchHistory();
            if (this.ngbBlastSearchService.blastHistory.length || this.ngbBlastSearchService.variantsPageError) {
                this.historyLoadingFinished();
            }
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(::this.$scope.$apply);
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    /*
        saveColumnsState() {
            if (!this.gridApi) {
                return;
            }
            const {columns} = this.gridApi.saveState.save();
            const mapNameToField = function ({name}) {
                switch (name) {
                    case 'Type':
                        return 'variationType';
                    case 'Chr':
                        return 'chrName';
                    case 'Gene':
                        return 'geneNames';
                    case 'Position':
                        return 'startIndex';
                    case 'Info':
                        return 'info';
                    default:
                        return name;
                }
            };
            const orders = columns.map(mapNameToField);
            const r = [];
            const names = this.projectContext.vcfColumns;
            for (let i = 0; i < names.length; i++) {
                const name = names[i];
                if (orders.indexOf(name) >= 0) {
                    r.push(1);
                } else {
                    r.push(0);
                }
            }
            let index = 0;
            const result = [];
            for (let i = 0; i < r.length; i++) {
                if (r[i] === 1) {
                    result.push(orders[index]);
                    index++;
                } else {
                    result.push(names[i]);
                }
            }
            this.projectContext.vcfColumns = result;
        }
    */
    historyLoadingFinished() {
        this.historyLoadError = null;
        this.gridOptions.columnDefs = this.ngbBlastHistoryTableService.getBlastHistoryGridColumns();
        const data = this.ngbBlastSearchService.blastHistory;
        this.gridOptions.totalItems = data.length;
        const firstRow = (this.ngbBlastSearchService.currentPageHistory - 1) * this.ngbBlastSearchService.historyPageSize;
        this.gridOptions.data = data.slice(firstRow, firstRow + this.ngbBlastSearchService.historyPageSize);
        this.isProgressShown = false;

        this.$timeout(::this.$scope.$apply);
    }

    getDataOnPage(page) {
        this.ngbBlastSearchService.firstPageHistory = page;
        this.ngbBlastSearchService.lastPageHistory = page;
        this.ngbBlastSearchService.currentPageHistory = page;
        this.gridOptions.data = [];
        this.ngbBlastSearchService.loadBlastHistory(page).then((data) => {
            if (this.ngbBlastHistoryTableService.historyPageError) {
                this.historyLoadError = this.ngbBlastHistoryTableService.historyPageError;
                this.gridOptions.totalItems = 0;
                this.gridOptions.data = [];
            } else {
                this.historyLoadError = null;
                this.gridOptions.totalItems = 100;
                const firstRow = (this.ngbBlastSearchService.currentPageHistory - 1) * this.ngbBlastSearchService.historyPageSize;
                this.gridOptions.data = data.slice(firstRow, firstRow + this.ngbBlastSearchService.historyPageSize);
            }
        });
    }

    sortChanged(grid, sortColumns) {
        // this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.projectContext.orderByVariations = sortColumns.map(sc => ({
                desc: sc.sort.direction === 'desc',
                field: this.projectContext.orderByColumnsVariations[sc.field] || sc.field
            }));
        } else {
            this.projectContext.orderByVariations = null;
        }

        this.ngbBlastSearchService.firstPageHistory = 1;
        this.ngbBlastSearchService.lastPageHistory = 1;

        this.gridOptions.data = [];
        this.ngbBlastSearchService.loadBlastHistory(1).then((data) => {
            if (this.ngbBlastHistoryTableService.historyPageError) {
                this.historyLoadError = this.ngbBlastHistoryTableService.historyPageError;
                this.gridOptions.totalItems = 0;
                this.gridOptions.data = [];
            } else {
                this.historyLoadError = null;
                this.gridOptions.totalItems = 100;
                const firstRow = (this.ngbBlastSearchService.currentPageHistory - 1) * this.ngbBlastSearchService.historyPageSize;
                this.gridOptions.data = data.slice(firstRow, firstRow + this.ngbBlastSearchService.historyPageSize);
            }
        });
    }

    changeCurrentPage(row) {
        this.$timeout(() => {
            if (row.newScrollTop) {
                const sizePage = this.ngbBlastSearchService.historyPageSize * ROW_HEIGHT;
                const currentPageVariations = Math.round(this.ngbBlastSearchService.firstPageHistory + row.newScrollTop / sizePage);
                if (this.ngbBlastSearchService.currentPageHistory !== currentPageVariations) {
                    this.ngbBlastSearchService.currentPageHistory = currentPageVariations;
                    this.dispatcher.emit('pageVariations:scroll', this.ngbBlastSearchService.currentPageHistory);
                }
            }
        });
    }

    onRemove(entity, event) {
        const confirm = this.$mdDialog.confirm()
            .title(`Delete history entry #${entity.id}?`)
            .ok('OK')
            .cancel('CANCEL');

        this.$mdDialog.show(confirm).then(async() => {
            await this.ngbBlastSearchService.deleteBlastHistory(entity.id);
            await this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }

    onCancel(entity, event) {
        const confirm = this.$mdDialog.confirm()
            .title(`Cancel search request #${entity.id}?`)
            .ok('OK')
            .cancel('CANCEL');

        this.$mdDialog.show(confirm).then(async() => {
            await this.ngbBlastSearchService.cancelBlastSearch(entity.id);
            await this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }

    onRepeat(entity, event) {
        this.ngbBlastSearchService.currentSearchId = entity.id;
        this.changeTab({tab: 'SEARCH'});
        event.stopImmediatePropagation();
        return false;
    }

    clearHistory(event) {
        const confirm = this.$mdDialog.confirm()
            .title('Clear all history?')
            .ok('OK')
            .cancel('CANCEL');
        this.$mdDialog.show(confirm).then(async() => {
            await this.ngbBlastSearchService.clearSearchHistory();
            await this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }

    showResult(entity, event) {
        this.ngbBlastSearchService.currentResultId = entity.id;
        this.changeTab({tab: 'RESULT'});
        event.stopImmediatePropagation();
        return false;
    }

    /*
            resizeGrid() {
                if (this.gridApi) {
                    this.gridApi.core.handleWindowResize();
                }
            }
        */
}
