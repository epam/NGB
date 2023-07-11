const OPEN_TARGETS_COLUMNS = ['target', 'drug', 'type', 'mechanism of action', 'action type', 'disease', 'phase', 'status', 'source'];
const PHARM_GKB_COLUMNS = ['target', 'drug', 'Source'];
const DGI_DB_COLUMNS = ['target', 'drug', 'interaction claim source', 'interaction types'];
export default class ngbDrugsTableController {

    get openTargetsColumnList () {
        return OPEN_TARGETS_COLUMNS;
    }
    get pharmGkbColumnList () {
        return PHARM_GKB_COLUMNS;
    }
    get dgiDbColumnList () {
        return DGI_DB_COLUMNS;
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

    constructor($scope, $timeout, dispatcher, ngbDrugsTableService, ngbKnownDrugsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbDrugsTableService, ngbKnownDrugsPanelService});

        const drugsSourceChanged = this.sourceChanged.bind(this);
        const diseasesSourceChanged = this.resetDrugsData.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('drugs:source:changed', drugsSourceChanged);
        dispatcher.on('diseases:source:changed', diseasesSourceChanged);
        dispatcher.on('drugs:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('drugs:source:changed', drugsSourceChanged);
            dispatcher.removeListener('diseases:source:changed', diseasesSourceChanged);
            dispatcher.removeListener('drugs:filters:changed', filterChanged);
        });
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
    get filterInfo() {
        return this.ngbDrugsTableService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbDrugsTableService.filterInfo = value;
    }

    resetDrugsData() {
        this.ngbDrugsTableService.resetDrugsData();
        this.dispatcher.emit('drugs:filters:reset');
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
            this.ngbDrugsTableService.setFieldList();
        }
    }

    async sourceChanged() {
        this.resetDrugsData();
        this.resetSorting();
        this.initialize();
        this.$timeout(::this.$scope.$apply);
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

    get sourceModel() {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }

    get sourceOptions() {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    getColumnList() {
        if (this.sourceModel === this.sourceOptions.OPEN_TARGETS) {
            return this.openTargetsColumnList;
        }
        if (this.sourceModel === this.sourceOptions.PHARM_GKB) {
            return this.pharmGkbColumnList;
        }
        if (this.sourceModel === this.sourceOptions.DGI_DB) {
            return this.dgiDbColumnList;
        }
    }

    getDrugsTableGridColumns() {
        const headerCells = require('./ngbDrugsTable_header.tpl.html');
        const linkCell = require('./ngbDrugsTable_linkCell.tpl.html');
        const targetCell = require('./ngbDrugsTable_targetCell.tpl.html');

        const result = [];
        const columnsList = this.getColumnList();
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
                case 'target':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: targetCell,
                    };
                    break;
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
                    if (this.sourceModel === this.sourceOptions.PHARM_GKB) {
                        columnSettings = {
                            ...columnSettings
                        };
                    } else {
                        columnSettings = {
                            ...columnSettings,
                            cellTemplate: linkCell
                        };
                    }
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
        const results = await this.ngbDrugsTableService.getDrugsResults()
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

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
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
