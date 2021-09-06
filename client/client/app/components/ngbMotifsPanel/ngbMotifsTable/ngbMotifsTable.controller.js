import baseController from '../../../shared/baseController';

export default class ngbMotifsTableController  extends baseController {

    gridOptions = {
        height: '100%',
        headerRowHeight: 20,
        rowHeight: 20,
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: false,
        enableRowSelection: true,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        treeRowHeaderAlwaysVisible: false,
    };
    _motifsSearchTitle = null;

    static get UID() {
        return 'ngbMotifsTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        projectContext,
        ngbMotifsPanelService,
        ngbMotifsTableService
    ) {
        super();
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            ngbMotifsPanelService,
            ngbMotifsTableService
        });
        this.gridOptions.rowHeight = this.ngbMotifsPanelService.rowHeight;
        this.dispatcher.on('motifs:search:change', ::this.backToParamsTable);
        this.dispatcher.on('motifs:pagination:next', ::this.getNextPage);
        this.dispatcher.on('motifs:pagination:previous', ::this.getPreviousPage);
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.ngbMotifsTableService.getMotifsGridColumns(),
            data: this.ngbMotifsPanelService.searchMotifsParams,
            appScopeProvider: this.$scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
            }
        });
    }

    get isShowParamsTable () {
        return this.ngbMotifsTableService.isShowParamsTable;
    }

    get hideTable () {
        return !this.isShowParamsTable && this.ngbMotifsPanelService.isSearchInProgress;
    }

    get motifsSearchTitle () {
        return this._motifsSearchTitle;
    }

    set motifsSearchTitle (row) {
        this._motifsSearchTitle = row.name ? row.name : row.motif;
    }

    rowClick(row) {
        if (this.ngbMotifsTableService.isShowParamsTable) {
            this.showResultsTable(row.entity);
        } else {
            this.ngbMotifsTableService.addTracks(row.entity);
        }
    }

    async showResultsTable (row) {
        this.ngbMotifsTableService.isShowParamsTable = false;
        this.motifsSearchTitle = row;
        const data = await this.ngbMotifsPanelService.resultsTableData(row)
            .then(success => {
                if (success) {
                    return this.ngbMotifsPanelService.searchMotifResults;
                }
            });
        if (data && data.length) {
            this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
            await this.loadData(data);
        }
    }

    backToParamsTable() {
        this.ngbMotifsTableService.isShowParamsTable = true;
        this.gridOptions.data = this.ngbMotifsPanelService.searchMotifsParams;
        this.gridOptions.columnDefs = this.ngbMotifsTableService.getMotifsGridColumns();
        this.ngbMotifsPanelService.currentParams = {};
    }

    async loadData (data) {
        if (data && data.length) {
            this.gridOptions.data = data;
            this.$timeout(::this.$scope.$apply);
        }
    }

    async getNextPage () {
        const data = await this.ngbMotifsPanelService.getNextResults()
            .then(success => {
                if (success) {
                    return this.ngbMotifsPanelService.searchMotifResults;
                }
            });
        await this.loadData(data);
    }

    async getPreviousPage () {
        const data = await this.ngbMotifsPanelService.getPreviousResults()
            .then(success => {
                if (success) {
                    return this.ngbMotifsPanelService.searchMotifResults;
                }
            });
        await this.loadData(data);
    }
}
