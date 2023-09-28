const GENOMICS_TABLE_COLUMNS = ['target', 'species', 'homology type', 'homologue', 'homology group', 'protein', 'aa', 'domains'];

export default class ngbGenomicsTableController {

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
            switch (column) {
                case 'target': {
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                    };
                    break;
                }
                case 'species': {
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                        cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                                        {{row.entity[col.field].name}}
                                    </div>`
                    };
                    break;
                }
                case 'homology type': {
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                    };
                    break;
                }
                case 'domains': {
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: '<ngb-homologs-domains domains="row.entity.domains"></ngb-homologs-domains>',
                        minWidth: 150,
                    };
                    break;
                }
                case 'homologue': {
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link">
                                           <a target="_blank" ng-href="https://www.ncbi.nlm.nih.gov/gene/{{row.entity.geneId}}">
                                               {{row.entity.homologue}}
                                           </a>
                                       </div>`,
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
        if (this.ngbGenomicsPanelService.genomicsData) {
            await this.ngbGenomicsPanelService.setGenomicsResults();
            await this.getDataOnPage(1);
            this.loadingData = false;
        } else {
            await this.ngbGenomicsPanelService.getGenomicsData()
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
        this.gridOptions.data = await this.ngbGenomicsPanelService.getGenomicsResults();
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
