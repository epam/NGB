export default class ngbPatentsChemicalTableController {

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
    };

    static get UID() {
        return 'ngbPatentsChemicalTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbPatentsChemicalsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbPatentsChemicalsTabService});
        const searchChanged = this.searchChanged.bind(this);
        dispatcher.on('target:identification:patents:drug:search:changed', searchChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:patents:drug:search:changed', searchChanged);
        });
    }

    get totalPages() {
        return this.ngbPatentsChemicalsTabService.totalPages;
    }
    get pageSize () {
        return this.ngbPatentsChemicalsTabService.pageSize;
    }
    set currentPage(value) {
        this.ngbPatentsChemicalsTabService.currentPage = value;
    }
    get loadingData() {
        return this.ngbPatentsChemicalsTabService.loadingData;
    }
    set loadingData(value) {
        this.ngbPatentsChemicalsTabService.loadingData = value;
    }
    get emptyResults() {
        return this.ngbPatentsChemicalsTabService.emptyResults;
    }
    get sortInfo() {
        return this.ngbPatentsChemicalsTabService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbPatentsChemicalsTabService.sortInfo = value;
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
        (this.initialize)();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        if (this.ngbPatentsChemicalsTabService.tableResults) {
            this.gridOptions.data = this.ngbPatentsChemicalsTabService.tableResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getPatentsTableGridColumns();
    }

    async searchChanged() {
        await this.initialize();
        this.$timeout(() => this.$scope.$apply());
    }

    getPatentsTableGridColumns() {
        const headerCells = require('./ngbPatentsChemicalTable_header.tpl.html');
        const linkCell = require('../../ngbPatentsTable_cells/ngbPatentsTable_linkCell.tpl.html');
        const tooltipCell = require('../../ngbPatentsTable_cells/ngbPatentsTable_tooltipCell.tpl.html');

        const result = [];
        const columnsList = this.ngbPatentsChemicalsTabService.getColumnList();
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
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'CID':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell,
                    };
                    break;
                case 'Name':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: tooltipCell
                    };
                    break;
                case 'IUPAC name':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: tooltipCell
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
        this.gridOptions.data = await this.ngbPatentsChemicalsTabService.getTableResults()
            .then(success => {
                if (success) {
                    return this.ngbPatentsChemicalsTabService.tableResults;
                }
                return [];
            });
        this.dispatcher.emit('target:identification:patents:drug:pagination:updated');
        this.$timeout(() => this.$scope.$apply());
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = page;
        await this.loadData();
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

    resetSorting() {
        if (!this.gridApi) {
            return;
        }
        const columns = this.gridApi.grid.columns;
        for (let i = 0 ; i < columns.length; i++) {
            columns[i].sort = {};
        }
    }
}
