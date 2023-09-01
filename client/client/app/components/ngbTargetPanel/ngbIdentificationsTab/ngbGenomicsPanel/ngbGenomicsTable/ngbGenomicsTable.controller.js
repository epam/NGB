const GENOMICS_TABLE_COLUMNS = ['species', 'homology type', 'homologue', 'query %id', 'target %id'];
export default class ngbGenomicsTableController {

    pageSize = 10;

    get genomicsTableColumns () {
        return GENOMICS_TABLE_COLUMNS;
    }

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
        saveSelection: false,
        useExternalSorting: false
    };

    static get UID() {
        return 'ngbGenomicsTableController';
    }

    constructor($scope, $timeout, ngbGenomicsPanelService) {
        Object.assign(this, {$scope, $timeout, ngbGenomicsPanelService});
    }

    get loadingData () {
        return this.ngbGenomicsPanelService.loadingData;
    }
    set loadingData (value) {
        this.ngbGenomicsPanelService.loadingData = value;
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: [],
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        this.initialize();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        this.gridOptions.columnDefs = this.getGenomicsTableGridColumns();
        await this.loadData();
    }

    getGenomicsTableGridColumns() {
        const headerCells = require('./ngbGenomicsTable_header.tpl.html');

        const result = [];
        const columnsList = this.genomicsTableColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                displayName: column,
                enableHiding: false,
                enableColumnMenu: false,
                enableSorting: false,
                enableFiltering: false,
                field: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    async loadData () {
        this.loadingData = true;
        this.gridOptions.data = [{
            'species': 'species',
            'homology type': 2,
            'homologue': 1,
            'query %id': 100,
            'target %id': 99
        }];
        await this.ngbGenomicsPanelService.getData();
        this.$timeout(() => this.$scope.$apply());
    }
}
