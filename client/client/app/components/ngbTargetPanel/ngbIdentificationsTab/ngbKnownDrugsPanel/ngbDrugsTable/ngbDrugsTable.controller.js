const DRUGS_TABLE_COLUMNS = ['drug', 'type', 'mechanism of action', 'action type', 'disease', 'phase', 'status', 'source'];

export default class ngbDrugsTableController {

    get drugsTableColumnList () {
        return DRUGS_TABLE_COLUMNS;
    }

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

    static get UID() {
        return 'ngbDrugsTableController';
    }

    constructor($scope, $timeout, ngbDrugsTableService) {
        Object.assign(this, {$scope, $timeout, ngbDrugsTableService});
    }

    get totalPages() {
        return this.ngbDrugsTableService.totalPages;
    }
    get currentPage() {
        return this.ngbDrugsTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbDrugsTableService.currentPage = value;
    }
    get loadingData() {
        return this.ngbDrugsTableService.loadingData;
    }
    set loadingData(value) {
        this.ngbDrugsTableService.loadingData = value;
    }
    get failedResult() {
        return this.ngbDrugsTableService.failedResult;
    }
    get errorMessageList() {
        return this.ngbDrugsTableService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbDrugsTableService.emptyResults;
    }
    get pageSize () {
        return this.ngbDrugsTableService.pageSize;
    }
    get sortInfo() {
        return this.ngbDrugsTableService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbDrugsTableService.sortInfo = value;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.getDrugsTableGridColumns(),
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        if (this.ngbDrugsTableService.drugsResults) {
            this.gridOptions.data = this.ngbDrugsTableService.drugsResults;
        } else {
            await this.loadData();
        }
    }

    getDrugsTableGridColumns() {
        const headerCells = require('./ngbDrugsTable_header.tpl.html');
        const linkCell = require('./ngbDrugsTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.drugsTableColumnList;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: false,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'drug':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                case 'disease':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                case 'source':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                default:
                    columnSettings = {
                        ...columnSettings,
                    };
                    break;
            }
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    getRequest() {
        return {
            page: this.currentPage,
            pageSize: this.pageSize
        };
    }

    async loadData () {
        this.loadingData = true;
        const request = this.getRequest();
        const results = await this.ngbDrugsTableService.postAssociatedDrugs(request)
            .then(success => {
                if (success) {
                    return this.ngbDrugsTableService.drugsResults;
                }
                return [];
            });
        this.gridOptions.data = results;
        this.$timeout(::this.$scope.$apply);
    }

    async sortChanged(grid, sortColumns) {
        if (!this.gridApi) {
            return;
        }
        if (sortColumns && sortColumns.length > 0) {
            this.sortInfo = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: sc.field
            }));
        } else {
            this.sortInfo = null;
        }
        this.currentPage = 1;
        this.gridOptions.data = [];
        await this.loadData();
        this.$timeout(::this.$scope.$apply);
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = page;
        await this.loadData();
    }

    onClickLink(row, event) {
        event.stopPropagation();
    }
}
