export default class ngbPatentsProteinTableController {

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
        return 'ngbPatentsProteinTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbPatentsSequencesTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbPatentsSequencesTabService});
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:patents:protein:changed', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:patents:protein:changed', refresh);
        });
    }

    get pageSize () {
        return this.ngbPatentsSequencesTabService.pageSize;
    }
    set currentPage(value) {
        this.ngbPatentsSequencesTabService.currentPage = value;
    }
    get loadingData() {
        return this.ngbPatentsSequencesTabService.loadingData;
    }
    set loadingData(value) {
        this.ngbPatentsSequencesTabService.loadingData = value;
    }
    get emptyResults() {
        return this.ngbPatentsSequencesTabService.emptyResults;
    }
    get sortInfo() {
        return this.ngbPatentsSequencesTabService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbPatentsSequencesTabService.sortInfo = value;
    }

    refresh() {
        if (this.ngbPatentsSequencesTabService.tableResults) {
            this.gridOptions.data = this.ngbPatentsSequencesTabService.tableResults;
        }
        this.$scope.$apply();
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
        if (this.ngbPatentsSequencesTabService.tableResults) {
            this.gridOptions.data = this.ngbPatentsSequencesTabService.tableResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getPatentsTableGridColumns();
    }

    async sourceChanged() {
        this.ngbPatentsSequencesTabService.resetTableResults();
        this.resetSorting();
        await this.initialize();
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

    getPatentsTableGridColumns() {
        const headerCells = require('./ngbPatentsProteinTable_header.tpl.html');
        const linkCell = require('./ngbPatentsProteinTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.ngbPatentsSequencesTabService.getColumnList();
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column.name,
                displayName: column.displayName,
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: false,
                field: column.name,
                headerTooltip: column.name,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column.name) {
                case 'protein':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell,
                    };
                    break;
                case 'length':
                    columnSettings = {
                        ...columnSettings,
                        minWidth: 20,
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
        this.gridOptions.data = await this.ngbPatentsSequencesTabService.getTableResults()
            .then(success => {
                if (success) {
                    return this.ngbPatentsSequencesTabService.tableResults;
                }
                return [];
            });
        this.dispatcher.emit('target:identification:patents:protein:results:updated');
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
}
