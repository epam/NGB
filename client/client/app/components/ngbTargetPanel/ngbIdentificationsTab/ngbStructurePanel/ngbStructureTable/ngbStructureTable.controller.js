export default class ngbStructureTableController {

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

    static get UID() {
        return 'ngbStructureTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbStructurePanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbStructurePanelService});
        const sourceChanged = this.sourceChanged.bind(this);
        dispatcher.on('target:identification:changed', sourceChanged);
        dispatcher.on('target:identification:structure:source:changed', sourceChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:changed', sourceChanged);
            dispatcher.removeListener('target:identification:structure:source:changed', sourceChanged);
        });
    }

    get totalPages() {
        return this.ngbStructurePanelService.totalPages;
    }
    get currentPage() {
        return this.ngbStructurePanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbStructurePanelService.currentPage = value;
    }
    get loadingData() {
        return this.ngbStructurePanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbStructurePanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbStructurePanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbStructurePanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbStructurePanelService.emptyResults;
    }
    get pageSize () {
        return this.ngbStructurePanelService.pageSize;
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
        (this.initialize)();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        if (this.ngbStructurePanelService.structureResults) {
            this.gridOptions.data = this.ngbStructurePanelService.structureResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getStructureTableGridColumns();
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = page;
        await this.loadData();
    }

    async sourceChanged() {
        this.resetStructureData();
        await this.initialize();
        this.$timeout(() => this.$scope.$apply());
    }

    getStructureTableGridColumns() {
        const headerCells = require('./ngbStructureTable_header.tpl.html');

        const result = [];
        const columnsList = this.ngbStructurePanelService.proteinDataBankColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
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
        this.gridOptions.data = await this.ngbStructurePanelService.getStructureResults()
            .then(success => {
                if (success) {
                    return this.ngbStructurePanelService.structureResults;
                }
                return [];
            });
        this.dispatcher.emit('target:identification:structure:results:updated');
        this.$timeout(() => this.$scope.$apply());
    }

    resetStructureData() {
        this.ngbStructurePanelService.resetStructureData();
    }
}
