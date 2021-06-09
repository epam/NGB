import * as Utilities from './../ngbBlastSearch.utilities';
import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbBlastHistoryController extends baseController {
    static get UID() {
        return 'ngbBlastHistoryController';
    }

    dispatcher;

    isProgressShown = true;
    isEmptyResult = false;
    errorMessageList = [];
    historyLoadError = null;
    updateInterval = null;
    statusViews = [];

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

    constructor($scope, $timeout, $interval, $mdDialog,
        ngbBlastHistoryTableService, ngbBlastSearchService, uiGridConstants,
        dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            $interval,
            $mdDialog,
            dispatcher,
            ngbBlastHistoryTableService,
            ngbBlastSearchService,
            uiGridConstants,
        });

        this.initEvents();
        this.$scope.$on('$destroy', () => {
            if (this.updateInterval) {
                this.$interval.cancel(this.updateInterval);
            }
        });
    }

    events = {
        'blast:history:page:change': ::this.getDataOnPage
    };

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.historyLoadError = null;

        const blastSearchState = this.ngbBlastHistoryTableService.blastSearchState;
        this.statusViews = {
            [blastSearchState.SEARCHING]: 'Searching...',
            [blastSearchState.DONE]: 'Done',
            [blastSearchState.FAILURE]: 'Failure',
            [blastSearchState.CANCELED]: 'Interrupted'
        };
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbBlastHistoryTableService.getBlastHistoryGridColumns(),
            paginationPageSize: this.ngbBlastHistoryTableService.historyPageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
                this.gridApi.core.on.gridDimensionChanged(this.$scope, ::Utilities.debounce(::this.onResize, RESIZE_DELAY));
            }
        });
        await this.loadData();
        this.updateInterval = this.$interval(::this.loadData, this.ngbBlastHistoryTableService.refreshInterval);
    }

    async loadData() {
        try {
            await this.ngbBlastHistoryTableService.updateSearchHistory();
            if (this.ngbBlastHistoryTableService.historyPageError) {
                this.historyLoadError = this.ngbBlastHistoryTableService.historyPageError;
                this.gridOptions.data = [];
                this.isEmptyResults = false;
            } else if (this.ngbBlastHistoryTableService.blastHistory.length) {
                this.historyLoadError = null;
                this.gridOptions.columnDefs = this.ngbBlastHistoryTableService.getBlastHistoryGridColumns();
                this.gridOptions.data = this.ngbBlastHistoryTableService.blastHistory;
                this.isEmptyResults = false;
            } else if (this.ngbBlastHistoryTableService.currentPageHistory > 1) {
                this.ngbBlastHistoryTableService.changePage(1);
            } else {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(::this.$scope.$apply);
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    rowClick(row, event) {
        const entity = row.entity;
        if (entity && entity.isResult) {
            this.ngbBlastSearchService.currentResultId = row.entity.id;
            this.changeState({state: 'RESULT'});
        } else {
            event.stopImmediatePropagation();
            return false;
        }
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const fieldTitleMap = (
            o => Object.keys(o).reduce(
                (r, k) => Object.assign(r, { [o[k]]: k }), {}
            )
        )(this.ngbBlastHistoryTableService.historyColumnTitleMap);
        const mapNameToField = function ({name}) {
            return fieldTitleMap[name];
        };
        const orders = columns.map(mapNameToField);
        const r = [];
        const names = this.ngbBlastHistoryTableService.blastHistoryColumns;
        for (const name of names) {
            r.push(orders.indexOf(name) >= 0);
        }
        let index = 0;
        const result = [];
        for (let i = 0; i < r.length; i++) {
            if (r[i]) {
                result.push(orders[index]);
                index++;
            } else {
                result.push(names[i]);
            }
        }
        this.ngbBlastHistoryTableService.blastHistoryColumns = result;
    }

    getDataOnPage(page) {
        this.ngbBlastHistoryTableService.firstPageHistory = page;
        this.loadData();
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.ngbBlastHistoryTableService.orderByHistory = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: this.ngbBlastHistoryTableService.orderByColumnsHistory[sc.field] || sc.field
            }));
        } else {
            this.ngbBlastHistoryTableService.orderByHistory = null;
        }

        this.ngbBlastHistoryTableService.currentPageHistory = 1;
        this.gridOptions.data = [];
        this.loadData();
    }

    onCancel(entity, event) {
        const confirm = this.$mdDialog.confirm()
            .title(`Cancel search request #${entity.id}?`)
            .ok('OK')
            .cancel('CANCEL');

        this.$mdDialog.show(confirm).then(async () => {
            await this.ngbBlastHistoryTableService.cancelBlastSearch(entity.id);
            await this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }

    onRepeat(entity, event) {
        this.ngbBlastSearchService.currentSearchId = entity.id;
        this.changeState({state: 'SEARCH'});
        event.stopImmediatePropagation();
        return false;
    }

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        this.ngbBlastHistoryTableService.historyPageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 1;
        this.loadData();
    }
}
