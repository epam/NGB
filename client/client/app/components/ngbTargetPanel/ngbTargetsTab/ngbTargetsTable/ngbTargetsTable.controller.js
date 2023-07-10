const TARGETS_TABLE_COLUMNS = ['name', 'genes', 'species', 'diseases', 'products', 'launch'];
const RESIZE_DELAY = 300;
export default class ngbTargetsTableController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 30,
        rowHeight: 'auto',
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: true,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: true,
        saveOrder: false,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: true,
        saveFilter: false,
        savePinning: false,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false
    };

    get targetsTableColumns() {
        return TARGETS_TABLE_COLUMNS;
    }

    get resizeDelay() {
        return RESIZE_DELAY;
    }

    get hideTable () {
        return this.loadingData || this.failedResult || this.emptyResults;
    }

    static get UID() {
        return 'ngbTargetsTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTableService, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbTargetsTableService, ngbTargetsTabService});

        const filterChanged = this.filterChanged.bind(this);
        const getDataOnPage = this.getDataOnPage.bind(this);
        const showTargetsTable = this.showTargetsTable.bind(this);
        const refreshTable = this.refreshTable.bind(this);
        dispatcher.on('targets:filters:changed', filterChanged);
        dispatcher.on('targets:pagination:changed', getDataOnPage);
        dispatcher.on('show:targets:table', showTargetsTable);
        dispatcher.on('target:launch:failed', refreshTable);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('targets:filters:changed', filterChanged);
            dispatcher.removeListener('targets:pagination:changed', getDataOnPage);
            dispatcher.removeListener('show:targets:table', showTargetsTable);
            dispatcher.removeListener('target:launch:failed', refreshTable);
        });
    }

    refreshTable() {
        this.$timeout(::this.$scope.$apply);
        this.$timeout(() => {
            this.launchFailed = false;
            this.launchErrorMessageList = null;
        }, 5000);
    }

    $onInit() {
        this.initialize();
    }

    get pageSize () {
        return this.ngbTargetsTableService.pageSize;
    }
    set pageSize (value) {
        this.ngbTargetsTableService.pageSize = value;
    }
    get totalCount() {
        return this.ngbTargetsTableService.totalCount;
    }
    get currentPage() {
        return this.ngbTargetsTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbTargetsTableService.currentPage = value;
    }
    get isLastPage() {
        return this.ngbTargetsTableService.isLastPage;
    }
    get failedResult() {
        return this.ngbTargetsTableService.failedResult;
    }
    get errorMessageList () {
        return this.ngbTargetsTableService.errorMessageList;
    }
    get displayFilters() {
        return this.ngbTargetsTableService.displayFilters;
    }
    get emptyResults() {
        return this.ngbTargetsTableService.emptyResults;
    }
    get filteringFailure() {
        return this.ngbTargetsTableService.filteringFailure;
    }
    get filteringErrorMessageList() {
        return this.ngbTargetsTableService.filteringErrorMessageList;
    }
    get sortInfo() {
        return this.ngbTargetsTableService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbTargetsTableService.sortInfo = value;
    }

    get loadingData() {
        return this.ngbTargetsTabService.tableLoading;
    }
    set loadingData(value) {
        this.ngbTargetsTabService.tableLoading = value;
    }

    get launchLoading() {
        return this.ngbTargetsTabService.launchLoading;
    }
    get launchFailed() {
        return this.ngbTargetsTabService.launchFailed;
    }
    set launchFailed(value) {
        this.ngbTargetsTabService.launchFailed = value;
    }
    get launchErrorMessageList() {
        return this.ngbTargetsTabService.launchErrorMessageList;
    }
    set launchErrorMessageList(value) {
        this.ngbTargetsTabService.launchErrorMessageList = value;
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.getTargetsTableGridColumns(),
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        const request = await this.ngbTargetsTableService.setGetTargetsRequest();
        await this.loadData(request);
    }

    getTargetsTableGridColumns() {
        const headerCells = require('./ngbTargetsTable_header.tpl.html');
        const launchCell = require('./ngbTargetsTable_launchCell.tpl.html');
        const nameCell = require('./ngbTargetsTable_nameCell.tpl.html');
        const geneCell = require('./ngbTargetsTable_geneCell.tpl.html');
        const defaultCell = require('./ngbTargetsTable_defaultCell.tpl.html');

        const result = [];
        const columnsList = this.targetsTableColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                enableHiding: false,
                enableColumnMenu: false,
                enableSorting: false,
                field: column,
                headerTooltip: column,
                width: '*'
            };
            switch (column) {
                case 'launch':
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: false,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        maxWidth: 50,
                        displayName: '',
                        cellTemplate: launchCell
                    };
                    break;
                case 'name':
                    columnSettings = {
                        ...columnSettings,
                        enableSorting: true,
                        enableFiltering: true,
                        enableColumnMenu: true,
                        headerCellTemplate: headerCells,
                        displayName: column,
                        cellTemplate: nameCell
                    };
                    break;
                case 'genes':
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        displayName: column,
                        cellTemplate: geneCell
                    };
                    break;
                default:
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        displayName: column,
                        cellTemplate: defaultCell
                    };
                    break;
            }
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    async rowClick(row, event) {
        await this.openTarget(row.entity, event);
    }

    async loadData (request) {
        const results = await this.ngbTargetsTableService.getTargetsResult(request)
            .then(success => {
                if (success) {
                    return this.ngbTargetsTableService.targetsResults;
                }
                return [];
            });
        this.gridOptions.data = results;
        this.loadingData = false;
        this.$timeout(::this.$scope.$apply);
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = page;
        this.gridOptions.data = [];
        const request = await this.ngbTargetsTableService.setGetTargetsRequest();
        await this.loadData(request);
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
        this.ngbTargetsTableService.currentPage = 1;
        this.gridOptions.data = [];
        const request = await this.ngbTargetsTableService.setGetTargetsRequest();
        await this.loadData(request);
        this.$timeout(::this.$scope.$apply);
    }

    launchTarget (row, event) {
        event.stopPropagation();
        this.dispatcher.emitSimpleEvent('target:launch:identification', row);
    }

    async openTarget (row, event) {
        event.stopPropagation();
        await this.ngbTargetsTabService.getTarget(row.id);
        this.$timeout(::this.$scope.$apply);
    }

    showOthers(cell, event) {
        event.stopPropagation();
        cell.limit = 100000;
    }

    showLess(cell, event) {
        event.stopPropagation();
        cell.limit = 2;
    }

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.ngbTargetsTableService.currentPage = 1;
        this.gridOptions.data = [];
        const request = await this.ngbTargetsTableService.setGetTargetsRequest();
        await this.loadData(request);
        this.$timeout(::this.$scope.$apply);
    }

    async showTargetsTable() {
        this.ngbTargetsTableService.currentPage = 1;
        const request = await this.ngbTargetsTableService.setGetTargetsRequest();
        await this.loadData(request);
    }
}
