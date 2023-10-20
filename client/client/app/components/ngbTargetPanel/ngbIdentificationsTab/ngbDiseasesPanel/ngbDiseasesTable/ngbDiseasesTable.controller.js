import ngbDiseasesControllerBase from '../ngbDiseases.controler.base';
import {
    SourceOptions
} from '../ngbDiseasesPanel.service';

const OPEN_TARGETS_COLUMNS = ['target', 'disease', 'overall score', 'genetic association', 'somatic mutations', 'drugs', 'pathways systems', 'text mining', 'animal models', 'RNA expression'];
const PHARM_GKB_COLUMNS = ['target', 'disease'];

const OPEN_TARGETS_DEFAULT_SORT = [{
    field: 'overall score',
    ascending: false
}]

export default class ngbDiseasesTableController extends ngbDiseasesControllerBase {

    get openTargetsColumnList () {
        return OPEN_TARGETS_COLUMNS;
    }
    get pharmGkbColumnList () {
        return PHARM_GKB_COLUMNS;
    }

    get openTargetsDefaultSort() {
        return OPEN_TARGETS_DEFAULT_SORT;
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
    };

    getHighlightColor(alpha) {
        return alpha
            ? {'background-color': `rgb(102, 153, 255, ${alpha})`}
            : undefined;
    }

    getColumnList() {
        if (this.sourceModel === SourceOptions.OPEN_TARGETS) {
            return this.openTargetsColumnList;
        }
        if (this.sourceModel === SourceOptions.PHARM_GKB) {
            return this.pharmGkbColumnList;
        }
        return [];
    }

    static get UID() {
        return 'ngbDiseasesTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesTableService,
        ngbDiseasesPanelService,
        ngbIdentificationsTabService
    ) {
        super($scope, $timeout, dispatcher);
        this.ngbDiseasesTableService = ngbDiseasesTableService;
        this.ngbDiseasesPanelService = ngbDiseasesPanelService;
        this.ngbIdentificationsTabService = ngbIdentificationsTabService;
        this.sourceOptions = SourceOptions;
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
    get filterInfo() {
        return this.ngbDiseasesTableService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbDiseasesTableService.filterInfo = value;
    }

    get sourceModel () {
        return this.ngbDiseasesPanelService.sourceModel;
    }

    resetDiseasesData() {
        this.ngbDiseasesTableService.resetDiseasesData();
        this.dispatcher.emit('diseases:filters:reset');
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            rowTemplate: require('./ngbDiseasesTable_row.tpl.html'),
            columnDefs: this.getDiseasesTableGridColumns(),
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
            }
        });
        await this.loadTableDataIfRequired();
    }

    async loadTableDataIfRequired() {
        if (!this.gridOptions) {
            return;
        }
        if (this.sourceModel === SourceOptions.OPEN_TARGETS) {
            this.sortInfo = this.openTargetsDefaultSort;
        }
        if (this.ngbDiseasesTableService.diseasesResults) {
            this.gridOptions.data = this.ngbDiseasesTableService.diseasesResults;
        } else {
            await this.loadData();
            this.ngbDiseasesTableService.setFieldList();
        }
        this.gridOptions.columnDefs = this.getDiseasesTableGridColumns();
    }

    async sourceChanged() {
        this.resetDiseasesData();
        this.resetSorting();
        await this.loadTableDataIfRequired();
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

    getDiseasesTableGridColumns() {
        const headerCells = require('./ngbDiseasesTable_header.tpl.html');
        const linkCell = require('./ngbDiseasesTable_linkCell.tpl.html');
        const colorCell = require('./ngbDiseasesTable_colorCell.tpl.html');

        const result = [];
        const columnsList = this.getColumnList();
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
                        enableFiltering: true,
                        minWidth: 200
                    };
                    break;
                case 'overall score':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: colorCell,
                        enableFiltering: true,
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
        this.gridOptions.data = await this.ngbDiseasesTableService.getDiseasesResults()
            .then(success => {
                if (success) {
                    return this.ngbDiseasesTableService.diseasesResults;
                }
                return [];
            });
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

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.currentPage = 1;
        await this.loadData();
        this.$timeout(() => this.$scope.$apply());
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
