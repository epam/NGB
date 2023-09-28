const COLUMN_LIST = ['target', 'target name', 'overall score', 'genetic association', 'somatic mutations', 'drugs', 'pathways systems', 'text mining', 'animal models', 'RNA expression'];

const DEFAULT_SORT = [{
    field: 'overall score',
    ascending: false
}];

export default class ngbDiseasesTargetsPanelController {

    get columnList () {
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
        enableHorizontalScrollbar: 1,
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
    }

    getHighlightColor(alpha) {
        return alpha
            ? {'background-color': `rgb(102, 153, 255, ${alpha})`}
            : undefined;
    }

    get defaultSort() {
        return DEFAULT_SORT;
    }

    static get UID() {
        return 'ngbDiseasesTargetsPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesTargetsPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesTargetsPanelService
        });
        const initialize = this.initialize.bind(this);
        this.dispatcher.on('target:diseases:targets:updated', initialize);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:targets:updated', initialize);
        });
    }

    get loadingData() {
        return this.ngbDiseasesTargetsPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbDiseasesTargetsPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbDiseasesTargetsPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbDiseasesTargetsPanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbDiseasesTargetsPanelService.emptyResults;
    }
    get pageSize () {
        return this.ngbDiseasesTargetsPanelService.pageSize;
    }
    get totalPages() {
        return this.ngbDiseasesTargetsPanelService.totalPages;
    }
    get currentPage() {
        return this.ngbDiseasesTargetsPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbDiseasesTargetsPanelService.currentPage = value;
    }
    get sortInfo() {
        return this.ngbDiseasesTargetsPanelService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbDiseasesTargetsPanelService.sortInfo = value;
    }
    get filterInfo() {
        return this.ngbDiseasesTargetsPanelService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbDiseasesTargetsPanelService.filterInfo = value;
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
        this.sortInfo = this.defaultSort;
        if (this.ngbDiseasesTargetsPanelService.targetsResults) {
            this.gridOptions.data = this.ngbDiseasesTargetsPanelService.targetsResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getTargetsTableGridColumns();
    }

    getTargetsTableGridColumns() {
        const headerCells = require('./ngbDiseasesTargetsTable_header.tpl.html');
        const colorCell = require('./ngbDiseasesTargetsTable_colorCell.tpl.html');

        const result = [];
        const columnsList = this.columnList;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                displayName: column.charAt(0).toUpperCase() + column.slice(1),
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: false,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 80,
                width: '*'
            };
            switch (column) {
                case 'target':
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                    };
                    break;
                case 'target name':
                    columnSettings = {
                        ...columnSettings,
                        enableFiltering: true,
                        minWidth: 200
                    };
                    break;
                default:
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: colorCell,
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
        this.gridOptions.data = await this.ngbDiseasesTargetsPanelService.getTargetsResults()
            .then(success => {
                if (success) {
                    return this.ngbDiseasesTargetsPanelService.targetsResults;
                }
                return [];
            });
        this.dispatcher.emit('target:diseases:targets:results:updated');
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
