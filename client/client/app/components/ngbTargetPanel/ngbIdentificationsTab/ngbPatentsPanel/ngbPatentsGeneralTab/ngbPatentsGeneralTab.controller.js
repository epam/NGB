import {GOOGLE_PATENTS_COLUMNS} from '../ngbPatentsChemicalsTab/ngbPatentsChemicalsTab.service';

export default class ngbPatentsGeneralTabController {
    static get UID() {
        return 'ngbPatentsGeneralTabController';
    }
    gridOptions = {
        height: '100%',
        headerRowHeight: 30,
        rowHeight: 'auto',
        showHeader: true,
        multiSelect: false,
        enableGridMenu: false,
        enableSorting: false,
        enableRowSelection: false,
        enableRowHeaderSelection: false,
        enableFiltering: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: false,
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
        saveSelection: false,
    };

    constructor($scope, $timeout, dispatcher, ngbPatentsGeneralTabService) {
        Object.assign(this, { $scope, $timeout, dispatcher, ngbPatentsGeneralTabService });
    }

    get loading() {
        return this.ngbPatentsGeneralTabService.loading;
    }

    get hasError() {
        return this.ngbPatentsGeneralTabService.errors && this.ngbPatentsGeneralTabService.errors.length > 0;
    }

    get errors() {
        return this.ngbPatentsGeneralTabService.errors;
    }

    get search() {
        return this.ngbPatentsGeneralTabService.search;
    }

    set search(search) {
        this.ngbPatentsGeneralTabService.search = search;
    }

    get searchDisabled() {
        return !this.search || this.search.trim().length === 0;
    }

    get page() {
        return this.ngbPatentsGeneralTabService.page + 1;
    }

    get totalPages() {
        return this.ngbPatentsGeneralTabService.totalPages;
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: [],
            paginationPageSize: this.ngbPatentsGeneralTabService.pageSize,
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
        this.applyResults();
        const tooltipCell = require('../ngbPatentsTable_cells/ngbPatentsTable_tooltipCell.tpl.html');
        this.gridOptions.columnDefs = GOOGLE_PATENTS_COLUMNS.map((column) => ({
            name: column.field,
            displayName: column.title,
            enableHiding: false,
            enableColumnMenu: false,
            enableSorting: false,
            enableFiltering: false,
            headerTooltip: column.title,
            cellTemplate: tooltipCell,
            minWidth: 40,
            width: '*',
            ...column,
        }));
    }

    onChangeSearch() {

    }

    onChangePage = (page) => {
        this.$timeout(async () => {
            await this.ngbPatentsGeneralTabService.getPageData(Math.max(0, page - 1));
            this.applyResults();
            this.$scope.$apply();
        });
    };

    async onClickSearch() {
        this.$timeout(async () => {
            await this.ngbPatentsGeneralTabService.performSearch();
            this.applyResults();
            this.$scope.$apply();
        });
    }

    applyResults() {
        if (this.ngbPatentsGeneralTabService) {
            this.gridOptions.data = this.ngbPatentsGeneralTabService.results || [];
        }
    }
}