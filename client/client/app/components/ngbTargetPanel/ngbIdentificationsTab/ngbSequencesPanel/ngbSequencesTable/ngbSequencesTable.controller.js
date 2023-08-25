const SEQUENCES_TABLE_COLUMNS = [{
    name: 'target',
    displayName: 'Target'
}, {
    name: 'transcript',
    displayName: 'Transcript'
}, {
    name: 'mrna length',
    displayName: 'Length (nt)'
}, {
    name: 'protein',
    displayName: 'Protein'
}, {
    name: 'protein length',
    displayName: 'Length (aa)'
}, {
    name: 'protein name',
    displayName: 'Protein name'
}];

export default class ngbSequencesTableController {

    get sequencesTableColumns () {
        return SEQUENCES_TABLE_COLUMNS;
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
        saveSelection: false,
        useExternalSorting: true
    };

    static get UID() {
        return 'ngbSequencesTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbSequencesTableService,
        ngbSequencesPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbSequencesTableService,
            ngbSequencesPanelService
        });

        const geneChanged = this.geneChanged.bind(this);
        dispatcher.on('target:identification:sequence:gene:changed', geneChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:sequence:gene:changed', geneChanged);
        });
    }

    get currentPage() {
        return this.ngbSequencesTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbSequencesTableService.currentPage = value;
    }

    get pageSize () {
        return this.ngbSequencesPanelService.pageSize;
    }
    get totalPages() {
        return this.ngbSequencesPanelService.totalPages;
    }
    get loadingData() {
        return this.ngbSequencesPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbSequencesPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbSequencesPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbSequencesPanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbSequencesPanelService.emptyResults;
    }
    get selectedGene() {
        return this.ngbSequencesPanelService.selectedGene;
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: [],
            paginationPageSize: this.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
            }
        });
        (this.initialize)();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        this.gridOptions.columnDefs = this.getSequencesTableGridColumns();
        if (this.ngbSequencesPanelService.sequencesResults) {
            await this.getDataOnPage(this.currentPage);
        } else {
            await this.loadData();
        }
    }

    getSequencesTableGridColumns() {
        const headerCells = require('./ngbSequencesTable_header.tpl.html');
        const linkCell = require('./ngbSequencesTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.sequencesTableColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column.name,
                displayName: column.displayName,
                enableHiding: false,
                enableColumnMenu: false,
                enableSorting: false,
                enableFiltering: false,
                field: column.name,
                headerTooltip: column.name,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column.name) {
                case 'transcript':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                case 'protein':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                case 'protein name':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: `<div class="ui-grid-cell-contents ng-binding ng-scope">
                            <md-tooltip>{{row.entity[col.field]}}</md-tooltip>
                            {{row.entity[col.field]}}
                        </div>`
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
        this.gridOptions.data = await this.ngbSequencesPanelService.getSequencesData()
            .then(success => {
                if (success) {
                    return this.ngbSequencesTableService.getSequencesResults();
                }
                return [];
            });
        this.dispatcher.emit('target:identification:sequences:results:updated');
        this.$timeout(() => this.$scope.$apply());
    }

    async getDataOnPage(page) {
        this.currentPage = page;
        this.gridOptions.data = this.ngbSequencesTableService.getSequencesResults();
        this.$timeout(() => this.$scope.$apply());
    }

    async geneChanged() {
        this.resetSequenceResults();
        await this.initialize();
    }

    resetSequenceResults() {
        this.currentPage = 1;
        this.ngbSequencesPanelService.resetSequenceResults();
    }
}
