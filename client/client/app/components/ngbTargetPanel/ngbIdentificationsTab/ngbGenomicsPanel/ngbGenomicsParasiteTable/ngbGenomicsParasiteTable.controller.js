const TABLE_COLUMNS = ['target', 'species', 'geneId', 'taxId'];

export default class ngbGenomicsParasiteTableController {

    get tableColumns () {
        return TABLE_COLUMNS;
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
        return 'ngbGenomicsParasiteTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbGenomicsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbGenomicsPanelService});
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:identification:genomics:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:genomics:filters:changed', filterChanged);
        });
    }

    get loadingData () {
        return this.ngbGenomicsPanelService.loadingData;
    }
    set loadingData (value) {
        this.ngbGenomicsPanelService.loadingData = value;
    }

    get currentPage() {
        return this.ngbGenomicsPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbGenomicsPanelService.currentPage = value;
    }
    get totalPages() {
        return this.ngbGenomicsPanelService.totalPages;
    }
    get emptyResults() {
        return this.ngbGenomicsPanelService.emptyResults;
    }
    get pageSize() {
        return this.ngbGenomicsPanelService.pageSize;
    }
    set filterInfo(value) {
        this.ngbGenomicsPanelService.filterInfo = value;
    }
    get defaultFilter() {
        return this.ngbGenomicsPanelService.defaultFilter;
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
        this.filterInfo = this.defaultFilter;
        await this.loadData();
    }

    getGenomicsTableGridColumns() {
        const headerCells = require('./ngbGenomicsParasiteTable_header.tpl.html');
        const translationalSpecies = this.ngbGenomicsPanelService.translationalSpecies;

        const result = [];
        const columnsList = this.tableColumns;
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
            switch (column) {
                case 'target': {
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                    };
                    break;
                }
                case 'species': {
                    const enableFiltering = translationalSpecies && translationalSpecies.length;
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: enableFiltering,
                        cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                                        {{row.entity[col.field].name}}
                                    </div>`
                    };
                    break;
                }
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

    async loadData () {
        this.loadingData = true;
        if (this.ngbGenomicsPanelService.genomicsParasiteData) {
            await this.getDataOnPage(1);
            this.loadingData = false;
        } else {
            await this.ngbGenomicsPanelService.getGenomicsParasiteData()
                .then(success => {
                    if (success) {
                        this.getDataOnPage(1);
                    }
                    this.gridOptions.data = [];
                });
        }
        this.dispatcher.emit('target:identification:genomics:results:updated');
        this.$timeout(() => this.$scope.$apply());
    }

    async getDataOnPage(page) {
        this.currentPage = page;
        this.gridOptions.data = await this.ngbGenomicsPanelService.getGenomicsParasiteResults();
        this.$timeout(() => this.$scope.$apply());
    }

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.currentPage = 1;
        await this.loadData();
        this.$timeout(() => this.$scope.$apply());
    }
}
