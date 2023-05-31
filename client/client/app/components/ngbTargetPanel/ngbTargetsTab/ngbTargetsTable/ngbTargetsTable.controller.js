const TARGETS_TABLE_COLUMNS = ['name', 'genes', 'species', 'disease', 'product', 'launch'];
const RESIZE_DELAY = 300;
export default class ngbTargetsTableController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 30,
        rowHeight: 'auto',
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: false,
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
        saveSort: false,
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
        dispatcher.on('targets:filter:changed', filterChanged);
        dispatcher.on('targets:pagination:changed', getDataOnPage);
        dispatcher.on('show:targets:table', showTargetsTable);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('targets:filter:changed', filterChanged);
            dispatcher.removeListener('targets:pagination:changed', getDataOnPage);
            dispatcher.removeListener('show:targets:table', showTargetsTable);
        });
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
    get loadingData() {
        return this.ngbTargetsTableService.loadingData;
    }
    set loadingData(value) {
        this.ngbTargetsTableService.loadingData = value;
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

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.getTargetsTableGridColumns(),
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
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
                        enableColumnMenu: false,
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
                        enableFiltering: true,
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

    launchTarget (row, event) {
        event.stopPropagation();
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
