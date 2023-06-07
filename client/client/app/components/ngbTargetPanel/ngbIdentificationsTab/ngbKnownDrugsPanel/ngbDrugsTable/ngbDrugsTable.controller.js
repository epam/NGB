const DRUGS_TABLE_COLUMNS = ['drug', 'type', 'mechanism of action', 'action type', 'disease', 'phase', 'status', 'source'];

const DRUGS_RESULTS = [
    {
        drug: 'SOTORASIB',
        type: 'Small molecule',
        'mechanism of action': 'GTRase KRas inhibitor',
        'action type': 'Inhibitor',
        disease: 'non-small cell lung carcinoma',
        phase: 'Phase IV',
        status: 'N/A',
        source: 'FDA'
    }, {
        drug: 'MRTX-849',
        type: 'Small molecule',
        'mechanism of action': 'GTRase KRas inhibitor',
        'action type': 'Inhibitor',
        disease: 'non-small cell lung carcinoma',
        phase: 'Phase III',
        status: 'Recruiting',
        source: 'ClinicalTrials.gov'
    }, {
        drug: 'MRTX-849',
        type: 'Small molecule',
        'mechanism of action': 'GTRase KRas inhibitor',
        'action type': 'Inhibitor',
        disease: 'metastatic colorectal cancer',
        phase: 'Phase III',
        status: 'Recruiting',
        source: 'ClinicalTrials.gov'
    }, {
        drug: 'SOTORASIB',
        type: 'Small molecule',
        'mechanism of action': 'GTRase KRas inhibitor',
        'action type': 'Inhibitor',
        disease: 'colorectal adenocarcinoma',
        phase: 'Phase III',
        status: 'Recruiting',
        source: 'ClinicalTrials.gov'
    }
];

export default class ngbDrugsTableController {

    get drugsTableColumnList () {
        return DRUGS_TABLE_COLUMNS;
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
        saveSelection: false
    };

    currentPage = 1;
    pagesTotal = 2;

    get isFirstPage () {
        return this.currentPage === 1;
    }
    get isLastPage () {
        return this.currentPage === this.pagesTotal;
    }

    static get UID() {
        return 'ngbDrugsTableController';
    }

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.getDrugsTableGridColumns(),
            paginationPageSize: 10,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        await this.loadData();
    }

    getDrugsTableGridColumns() {
        const headerCells = require('./ngbDrugsTable_header.tpl.html');

        const result = [];
        const columnsList = this.drugsTableColumnList;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: false,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    async loadData () {
        const results = DRUGS_RESULTS;
        this.gridOptions.data = results;
        this.$timeout(::this.$scope.$apply);
    }

    sortChanged() {}

    async getNextPage() {
        if (this.isLastPage) return;
        this.currentPage += 1;
        await this.getDataOnPage();
    }

    async getPreviousPage() {
        if (this.isFirstPage) return;
        this.currentPage -= 1;
        await this.getDataOnPage();
    }

    async getDataOnPage() {
        if (!this.gridApi) {
            return;
        }
        await this.loadData();
    }
}
