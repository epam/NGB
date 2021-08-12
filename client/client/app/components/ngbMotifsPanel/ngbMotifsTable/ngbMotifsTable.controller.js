import baseController from '../../../shared/baseController';

export default class ngbMotifsTableController  extends baseController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 20,
        rowHeight: 20,
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: true,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        treeRowHeaderAlwaysVisible: false,
    };
    _motifPattern = null;
    currentPage = 0;
    pattern = null;
    rowNumber = null;

    static get UID() {
        return 'ngbMotifsTableController';
    }

    constructor(
        $scope,
        dispatcher,
        ngbMotifsPanelService,
        ngbMotifsTableService
    ) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbMotifsPanelService,
            ngbMotifsTableService
        });
        this.gridOptions.rowHeight = this.ngbMotifsPanelService.rowHeight;
        this.dispatcher.on('motifs:search:change', ::this.backToFirstLevel);
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            data: this.ngbMotifsPanelService.firstLevelData,
            appScopeProvider: this.$scope,
            columnDefs: this.ngbMotifsTableService.getMotifsGridColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
    }

    get isLevelFirst () {
        return this.ngbMotifsTableService.isLevelFirst;
    }

    get isLevelSecond () {
        return this.ngbMotifsTableService.isLevelSecond;
    }

    get motifPattern () {
        return this._motifPattern;
    }

    set motifPattern (row) {
        this._motifPattern = row.name ? row.name : row.motif;
    }

    get totalPages () {
        const dataLength = this.ngbMotifsPanelService.getDataLength(this.rowNumber);
        const pageSize = this.ngbMotifsPanelService.pageSize;
        return Math.ceil(dataLength / pageSize);
    }

    rowClick(row) {
        if (this.isLevelFirst) {
            this.showResults(row.entity);
        } else {
            this.ngbMotifsTableService.addTracks({id: this.rowNumber, motif: this.motifPattern, ...row.entity});
        }
    }

    showResults (row) {
        this.rowNumber = row.id;
        this.pattern = row;
        this.ngbMotifsTableService.isLevelFirst = false;
        this.motifPattern = row;
        this.currentPage = 1;
        const data = this.ngbMotifsPanelService.secondLevelData(row, this.currentPage);
        this.gridOptions.data = data;
        this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
    }

    backToFirstLevel() {
        this.ngbMotifsTableService.isLevelFirst = true;
        this.gridOptions.data = this.ngbMotifsPanelService.firstLevelData;
        this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
    }

    sortChanged(grid, sortColumns) {
        this.gridApi.saveState.save();
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
        this.changePage(1);
    }

    changePage (page) {
        this.currentPage = page;
        const data = this.ngbMotifsPanelService.secondLevelData(this.pattern, this.currentPage);
        this.gridOptions.data = data;
    }
}
