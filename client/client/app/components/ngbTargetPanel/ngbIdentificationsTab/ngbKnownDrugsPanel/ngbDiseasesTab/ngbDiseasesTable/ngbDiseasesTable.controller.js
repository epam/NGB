const DISEASES_TABLE_COLUMNS = ['target', 'disease', 'overall score', 'genetic association', 'somatic mutations', 'drugs', 'pathways systems', 'text mining', 'animal models', 'RNA expression'];

export default class ngbDiseasesTableController {

    get diseasesTableColumnList () {
        return DISEASES_TABLE_COLUMNS;
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
        saveSelection: false
    };

    getHighlightColor(alpha) {
        return alpha
            ? {'background-color': `rgb(102, 153, 255, ${alpha})`}
            : undefined;
    }

    static get UID() {
        return 'ngbDiseasesTableController';
    }

    constructor($scope, $timeout, dispatcher, ngbDiseasesTableService) {
        Object.assign(this, {$scope, $timeout, ngbDiseasesTableService});

        const diseasesSourceChanged = this.sourceChanged.bind(this);
        const drugsSourceChanged = this.resetDiseasesData.bind(this);
        dispatcher.on('diseases:source:changed', diseasesSourceChanged);
        dispatcher.on('drugs:source:changed', drugsSourceChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('diseases:source:changed', diseasesSourceChanged);
            dispatcher.removeListener('drugs:source:changed', drugsSourceChanged);
        });
    }

    get totalPages() {
        return this.ngbDiseasesTableService.totalPages;
    }
    get currentPage() {
        return this.ngbDiseasesTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbDiseasesTableService.currentPage = value;
    }
    get loadingData() {
        return this.ngbDiseasesTableService.loadingData;
    }
    set loadingData(value) {
        this.ngbDiseasesTableService.loadingData = value;
    }
    get failedResult() {
        return this.ngbDiseasesTableService.failedResult;
    }
    get errorMessageList() {
        return this.ngbDiseasesTableService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbDiseasesTableService.emptyResults;
    }
    get pageSize () {
        return this.ngbDiseasesTableService.pageSize;
    }
    get sortInfo() {
        return this.ngbDiseasesTableService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbDiseasesTableService.sortInfo = value;
    }

    resetDiseasesData() {
        this.ngbDiseasesTableService.resetDiseasesData();
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.getDiseasesTableGridColumns(),
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        if (this.ngbDiseasesTableService.diseasesResults) {
            this.gridOptions.data = this.ngbDiseasesTableService.diseasesResults;
        } else {
            await this.loadData();
        }
    }

    async sourceChanged() {
        this.resetDiseasesData();
        this.initialize();
        this.$timeout(::this.$scope.$apply);
    }

    getDiseasesTableGridColumns() {
        const headerCells = require('../../cellTemplates/ngbDrugs&DiseasesTable_header.tpl.html');
        const linkCell = require('../../cellTemplates/ngbDrugs&DiseasesTable_linkCell.tpl.html');
        const colorCell = require('./ngbDiseasesTable_colorCell.tpl.html');

        const result = [];
        const columnsList = this.diseasesTableColumnList;
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
                case 'disease':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell,
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
        const results = await this.ngbDiseasesTableService.getDiseasesResults()
            .then(success => {
                if (success) {
                    return this.ngbDiseasesTableService.diseasesResults;
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
