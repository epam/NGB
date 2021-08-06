import baseController from '../../../../shared/baseController';
import {Debounce} from '../../../../shared/utils/debounce';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbHomologeneResultTableController extends baseController {
    dispatcher;
    isProgressShown = true;
    isEmptyResults = false;
    errorMessageList = [];
    searchResultTableLoadError = null;
    debounce = (new Debounce()).debounce;
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
        'homologs:homologene:result:page:change': ::this.getDataOnPage
    };

    constructor($scope, $timeout, ngbHomologeneResultService, ngbHomologsService, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbHomologeneResultService,
            ngbHomologsService
        });

        this.initEvents();
        this.$scope.$on('$destroy', () => {
            this.ngbHomologsService.isEmptyResults = true;
        });
    }

    static get UID() {
        return 'ngbHomologeneResultTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        this.ngbHomologeneResultService.currentPage = 1;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbHomologeneResultService.getHomologeneResultGridColumns(),
            paginationCurrentPage: this.ngbHomologeneResultService.currentPage,
            paginationPageSize: this.ngbHomologeneResultService.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.core.on.gridDimensionChanged(this.$scope, this.debounce(this, this.onResize.bind(this), RESIZE_DELAY));
            }
        });
        await this.loadData();
    }

    async loadData() {
        try {
            await this.ngbHomologeneResultService.updateSearchResult(this.ngbHomologsService.currentResultId);
            const resultLength = this.ngbHomologeneResultService.homologeneResult.length;
            if (this.ngbHomologeneResultService.searchResultTableError) {
                this.searchResultTableLoadError = this.ngbHomologeneResultService.searchResultTableError;
                this.gridOptions.data = [];
                this.gridOptions.totalItems = 0;
                this.isEmptyResults = false;
            } else if (resultLength) {
                this.searchResultTableLoadError = null;
                this.gridOptions.data = this.ngbHomologeneResultService.homologeneResult;
                this.gridOptions.paginationPageSize = this.ngbHomologeneResultService.pageSize;
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
        this.ngbHomologeneResultService.firstPage = page;
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
        const names = this.ngbHomologeneResultService.homologeneResultColumns;
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
        this.ngbHomologeneResultService.homologeneResultColumns = result;
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

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        const pageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 1;
        this.ngbHomologeneResultService.pageSize = pageSize;
        this.gridOptions.paginationPageSize = pageSize;
        this.$timeout(() => this.$scope.$apply());
    }
}
