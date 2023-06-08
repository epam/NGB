const DISEASES_TABLE_COLUMNS = ['disease', 'overall score', 'genetic association', 'somatic mutations', 'drugs', 'pathways systems', 'text mining', 'animal models'];

const DISEASES_RESULTS = [
    {
        disease: 'Cardiofaciocutaneous syndrome',
        'overall score': 0.83,
        'genetic association': 0.92,
        'somatic mutations': '',
        drugs: '',
        'pathways systems': 0.61,
        'text mining': '',
        'animal models': ''
    }, {
        disease: 'Noonan syndrome',
        'overall score': 0.83,
        'genetic association': 0.91,
        'somatic mutations': '',
        drugs: '',
        'pathways systems': 0.92,
        'text mining': '',
        'animal models': ''
    }, {
        disease: 'Non-small cell lung carcinoma',
        'overall score': 0.73,
        'genetic association': '',
        'somatic mutations': 0.80,
        drugs: 0.75,
        'pathways systems': 0.76,
        'text mining': 0.96,
        'animal models': ''
    }, {
        disease: 'Gastric cancer',
        'overall score': 0.70,
        'genetic association': 0.79,
        'somatic mutations': 0.55,
        drugs: '',
        'pathways systems': '',
        'text mining': '',
        'animal models': ''
    }, {
        disease: 'Acute myeloid leukemia',
        'overall score': 0.69,
        'genetic association': 0.73,
        'somatic mutations': 0.72,
        drugs: '',
        'pathways systems': 0.91,
        'text mining': 0.62,
        'animal models': 0.38
    }, {
        disease: 'Acute myeloid leukemia',
        'overall score': 0.20,
        'genetic association': 0.10,
        'somatic mutations': 1,
        drugs: 0.33,
        'pathways systems': 0.5,
        'text mining': 0.24,
        'animal models': 0.01
    }, {
        disease: 'Acute myeloid leukemia',
        'overall score': 1.00,
        'genetic association': 0.99,
        'somatic mutations': 0.98,
        drugs: 0.97,
        'pathways systems': 0.95,
        'text mining': 0.94,
        'animal models': 0.91
    }
];

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
    totalPages = 20;

    getHighlightColor(alpha) {
        return alpha
            ? {'background-color': `rgb(102, 153, 255, ${alpha})`}
            : undefined;
    }

    static get UID() {
        return 'ngbDiseasesTableController';
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
        const headerCells = require('./ngbDiseasesTable_header.tpl.html');
        const linkCell = require('./ngbDiseasesTable_linkCell.tpl.html');
        const colorCell = require('./ngbDiseasesTable_colorCell.tpl.html');

        const result = [];
        const columnsList = this.diseasesTableColumnList;
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
            switch (column) {
                case 'disease':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                default:
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: colorCell
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
        const results = DISEASES_RESULTS;
        this.gridOptions.data = results;
        this.$timeout(::this.$scope.$apply);
    }

    sortChanged() {}

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        await this.loadData();
    }

    onClickLink(row, event) {
        event.stopPropagation();
    }
}
