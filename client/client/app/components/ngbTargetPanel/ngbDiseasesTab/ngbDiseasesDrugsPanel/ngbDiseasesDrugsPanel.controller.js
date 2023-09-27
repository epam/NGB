const COLUMN_LIST = ['disease', 'drug', 'type', 'mechanism of action', 'action type', 'target', 'phase', 'status', 'source'];

export default class ngbDiseasesDrugsPanelController {

    get columnList() {
        return COLUMN_LIST;
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
        saveSelection: false,
        useExternalSorting: true
    };

    static get UID() {
        return 'ngbDiseasesDrugsPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesDrugsPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesDrugsPanelService
        });
        const filterChanged = this.filterChanged.bind(this);
        const initialize = this.initialize.bind(this);
        this.dispatcher.on('target:diseases:drugs:updated', initialize);
        dispatcher.on('target:diseases:drugs:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:drugs:updated', initialize);
            dispatcher.removeListener('target:diseases:drugs:filters:changed', filterChanged);
        });
    }

    get loadingData() {
        return this.ngbDiseasesDrugsPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbDiseasesDrugsPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbDiseasesDrugsPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbDiseasesDrugsPanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbDiseasesDrugsPanelService.emptyResults;
    }
    get pageSize () {
        return this.ngbDiseasesDrugsPanelService.pageSize;
    }
    get totalPages() {
        return this.ngbDiseasesDrugsPanelService.totalPages;
    }
    get currentPage() {
        return this.ngbDiseasesDrugsPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbDiseasesDrugsPanelService.currentPage = value;
    }
    get sortInfo() {
        return this.ngbDiseasesDrugsPanelService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbDiseasesDrugsPanelService.sortInfo = value;
    }
    get filterInfo() {
        return this.ngbDiseasesDrugsPanelService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbDiseasesDrugsPanelService.filterInfo = value;
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: [],
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
            }
        });
        this.initialize();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        this.resetSorting();
        if (this.ngbDiseasesDrugsPanelService.drugsResults) {
            this.gridOptions.data = this.ngbDiseasesDrugsPanelService.drugsResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getDrugsTableGridColumns();
    }

    getDrugsTableGridColumns() {
        const headerCells = require('./ngbDiseasesDrugsTable_header.tpl.html');
        const linkCell = require('./ngbDiseasesDrugsTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.columnList;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: true,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'disease':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell,
                    };
                    break;
                case 'drug':
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

    async loadData () {
        this.loadingData = true;
        this.gridOptions.data = await this.ngbDiseasesDrugsPanelService.getDrugsResults()
            .then(success => {
                if (success) {
                    return this.ngbDiseasesDrugsPanelService.drugsResults;
                }
                return [];
            });
        this.dispatcher.emit('target:diseases:drugs:results:updated');
        this.$timeout(() => this.$scope.$apply());
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
        this.currentPage = 1;
        await this.loadData();
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

    resetSorting() {
        if (!this.gridApi) {
            return;
        }
        const columns = this.gridApi.grid.columns;
        for (let i = 0 ; i < columns.length; i++) {
            columns[i].sort = {};
        }
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
