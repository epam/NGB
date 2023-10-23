export default class ngbStructureTableController {

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
        return 'ngbStructureTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbStructurePanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbStructurePanelService});
        const sourceChanged = this.sourceChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:identification:structure:source:changed', sourceChanged);
        dispatcher.on('target:identification:structure:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:structure:source:changed', sourceChanged);
            dispatcher.removeListener('target:identification:structure:filters:changed', filterChanged);
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

    get filterInfo() {
        return this.ngbStructurePanelService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbStructurePanelService.filterInfo = value;
    }
    set descriptionDone(value) {
        this.ngbStructurePanelService.descriptionDone = value;
    }
    get structureResults() {
        return this.ngbStructurePanelService.structureResults;
    }
    get sortInfo() {
        return this.ngbStructurePanelService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbStructurePanelService.sortInfo = value;
    }

    get sourceModel() {
        return this.ngbStructurePanelService.sourceModel;
    }
    get sourceOptions() {
        return this.ngbStructurePanelService.sourceOptions;
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
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
            }
        });
        (this.initialize)();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        if (this.structureResults) {
            this.gridOptions.data = this.structureResults;
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

    onClickLink(event) {
        if (event) {
            event.stopPropagation();
        }
    }

    async rowClick(row) {
        this.descriptionDone = false;
        await this.ngbStructurePanelService.getPdbDescription(row.entity);
        this.$timeout(() => {
            this.descriptionDone = true;
        });
    }

    async sourceChanged() {
        this.resetStructureData();
        this.resetSorting();
        await this.initialize();
        this.$timeout(() => this.$scope.$apply());
    }

    getStructureTableGridColumns() {
        this.gridOptions.columnDefs = [];
        const headerCells = require('./ngbStructureTable_header.tpl.html');
        const linkCell = require('./ngbStructureTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.ngbStructurePanelService.columnsList;
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
                headerTooltip: column,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'id':
                    if (this.sourceModel === this.sourceOptions.LOCAL_FILES) {
                        columnSettings = {
                            ...columnSettings,
                            enableFiltering: true,
                            enableSorting: true,
                            enableColumnMenu: true,
                            cellTemplate: linkCell
                        };
                    }
                    if (this.sourceModel === this.sourceOptions.PROTEIN_DATA_BANK) {
                        columnSettings = {
                            ...columnSettings,
                            enableFiltering: true,
                            cellTemplate: linkCell
                        };
                    }
                    break;
                case 'name':
                    if (this.sourceModel === this.sourceOptions.LOCAL_FILES) {
                        columnSettings = {
                            ...columnSettings,
                            enableFiltering: true,
                            enableSorting: true,
                            enableColumnMenu: true,
                            cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                                <md-tooltip>{{row.entity[col.field]}}</md-tooltip>
                                {{row.entity[col.field]}}
                            </div>`
                        };
                    }
                    if (this.sourceModel === this.sourceOptions.PROTEIN_DATA_BANK) {
                        columnSettings = {
                            ...columnSettings,
                            enableFiltering: true,
                            cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                                <md-tooltip>{{row.entity[col.field]}}</md-tooltip>
                                {{row.entity[col.field]}}
                            </div>`
                        };
                    }
                    break;
                case 'method':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                            <md-tooltip>{{row.entity[col.field]}}</md-tooltip>
                            {{row.entity[col.field]}}
                        </div>`
                    };
                    break;
                case 'owner':
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                        enableSorting: true,
                        enableColumnMenu: true,
                    };
                    break;
                case 'chains':
                    columnSettings = {
                        ...columnSettings,
                        width: 80
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
        this.currentPage = 1;
        await this.loadData();
    }


    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = 1;
        await this.loadData();
    }

    async loadData () {
        this.loadingData = true;
        this.gridOptions.data = await this.ngbStructurePanelService.getStructureResults()
            .then(success => {
                if (success) {
                    return this.structureResults;
                }
                return [];
            });
        this.dispatcher.emit('target:identification:structure:results:updated');
        this.$timeout(() => this.$scope.$apply());
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

    resetStructureData() {
        this.ngbStructurePanelService.resetStructureData();
        this.dispatcher.emit('target:identification:structure:filters:reset');
    }
}
