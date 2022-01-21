import baseController from '../../../shared/baseController';

const MOTIFS_PARAMS_COLUMNS = ['name', 'motif', 'search type'];

export default class ngbMotifsParamsTableController  extends baseController {

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
        saveSelection: false
    };

    get motifsParamsColumns() {
        return MOTIFS_PARAMS_COLUMNS;
    }

    static get UID() {
        return 'ngbMotifsParamsTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbMotifsPanelService) {
        super();
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbMotifsPanelService});
        this.gridOptions.rowHeight = this.ngbMotifsPanelService.rowHeight;
        this.dispatcher.on('motifs:show:params', this.showParams.bind(this));
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            columnDefs: this.getMotifsParamsGridColumns(),
            data: this.ngbMotifsPanelService.searchMotifsParams,
            appScopeProvider: this.$scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
            }
        });
    }

    getMotifsParamsGridColumns () {
        const headerCells = require('../ngbMotifsTable_header.tpl.html');

        const result = [];
        const columnsList = this.motifsParamsColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                enableHiding: false,
                enableFiltering: false,
                enableSorting: false,
                field: column,
                headerCellTemplate: headerCells,
                headerTooltip: column,
                minWidth: 40,
                displayName: column,
                width: '*'
            };
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    showParams() {
        this.gridOptions.data = this.ngbMotifsPanelService.searchMotifsParams;
        this.$timeout(::this.$scope.$apply);
    }

    rowClick(row) {
        this.ngbMotifsPanelService.searchMotif(row.entity);
    }
}
