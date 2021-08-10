import baseController from '../../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbOrthoParaResultTableController extends baseController {
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
        enablePaginationControls: false,
    };
    events = {
        'homologs:orthoPara:result:page:change': ::this.getDataOnPage
    };

    constructor($scope, $timeout, ngbOrthoParaResultService, ngbHomologsService, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbOrthoParaResultService,
            ngbHomologsService
        });

        this.initEvents();
        this.$scope.$on('$destroy', () => {
            this.ngbHomologsService.isEmptyResults = true;
        });
    }

    static get UID() {
        return 'ngbOrthoParaResultTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        Object.assign(this.gridOptions, {
            paginationCurrentPage: this.ngbOrthoParaResultService.currentPage,
            appScopeProvider: this.$scope,
            columnDefs: this.ngbOrthoParaResultService.getOrthoParaResultGridColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
            }
        });
        await this.loadData();
    }

    async loadData() {
        try {
            await this.ngbOrthoParaResultService.updateSearchResult(this.ngbHomologsService.currentResultId);
            const resultLength = this.ngbOrthoParaResultService.orthoParaResult.length;
            if (this.ngbOrthoParaResultService.searchResultTableError) {
                this.searchResultTableLoadError = this.ngbOrthoParaResultService.searchResultTableError;
                this.gridOptions.data = [];
                this.gridOptions.totalItems = 0;
                this.isEmptyResults = false;
            } else if (resultLength) {
                this.searchResultTableLoadError = null;
                this.gridOptions.data = this.ngbOrthoParaResultService.orthoParaResult;
                this.gridOptions.paginationPageSize = this.ngbOrthoParaResultService.pageSize;
                this.gridOptions.totalItems = resultLength;
                this.isEmptyResults = false;
            } else {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
            this.ngbHomologsService.isEmptyResults = this.isEmptyResults;
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(() => this.$scope.$apply());
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    getDataOnPage(page) {
        this.ngbOrthoParaResultService.firstPage = page;
        if (this.gridApi) {
            this.gridApi.pagination.seek(page);
        }
        return this.loadData();
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const orders = columns.map(c => c.name);
        const r = [];
        const names = this.ngbOrthoParaResultService.orthoParaResultColumns;
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
        this.ngbOrthoParaResultService.orthoParaResultColumns = result;
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
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
    }
}
