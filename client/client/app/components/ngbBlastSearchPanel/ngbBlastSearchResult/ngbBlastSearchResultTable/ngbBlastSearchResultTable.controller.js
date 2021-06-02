import baseController from '../../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbBlastSearchResultTableController extends baseController {
    static get UID() {
        return 'ngbBlastSearchResultTableController';
    }

    dispatcher;

    isProgressShown = true;
    isEmptyResults = false;
    errorMessageList = [];
    searchResultTableLoadError = null;

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

    constructor($scope, $timeout, ngbBlastSearchResultTableService, ngbBlastSearchService, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchResultTableService,
            ngbBlastSearchService
        });

        this.initEvents();
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbBlastSearchResultTableService.getBlastSearchResultGridColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
            }
        });
        await this.loadData();
    }

    async loadData() {
        try {
            await this.ngbBlastSearchResultTableService.updateSearchResult(this.ngbBlastSearchService.currentResultId);
            if (this.ngbBlastSearchResultTableService.searchResultTableError) {
                this.searchResultTableLoadError = this.ngbBlastSearchResultTableService.searchResultTableError;
                this.gridOptions.data = [];
                this.isEmptyResults = false;
            } else if (this.ngbBlastSearchResultTableService.blastSearchResult.length) {
                this.searchResultTableLoadError = null;
                this.gridOptions.columnDefs = this.ngbBlastSearchResultTableService.getBlastSearchResultGridColumns();
                this.gridOptions.data = this.ngbBlastSearchResultTableService.blastSearchResult;
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

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const orders = columns.map(c => c.name);
        const r = [];
        const names = this.ngbBlastSearchResultTableService.blastSearchResultColumns;
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
        this.ngbBlastSearchResultTableService.blastSearchResultColumns = result;
    }
}
