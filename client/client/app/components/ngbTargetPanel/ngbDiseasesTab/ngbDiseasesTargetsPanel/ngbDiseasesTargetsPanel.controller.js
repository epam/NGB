const COLUMN_LIST = ['target', 'target name', 'homologues', 'overall score', 'genetic association', 'somatic mutations', 'drugs', 'pathways systems', 'text mining', 'animal models', 'RNA expression'];

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

    getCellStyle(alpha) {
        return alpha
            ? {
                'background-color': `rgb(102, 153, 255, ${alpha})`,
                'height': '100%'
            }
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
        ngbDiseasesTargetsPanelService,
        ngbDiseasesTabService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesTargetsPanelService,
            ngbDiseasesTabService
        });
        const diseaseChanged = this.diseaseChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:diseases:disease:changed', diseaseChanged);
        dispatcher.on('target:diseases:targets:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:disease:changed', diseaseChanged);
            dispatcher.removeListener('target:diseases:targets:filters:changed', filterChanged);
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
    get targetsResults() {
        return this.ngbDiseasesTargetsPanelService.targetsResults;
    }
    get diseaseName() {
        return (this.ngbDiseasesTabService.diseasesData || {}).name;
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
        this.sortInfo = this.defaultSort;
        await this.ngbDiseasesTargetsPanelService.setDefaultFilter();
        if (this.targetsResults) {
            this.gridOptions.data = this.targetsResults;
        } else {
            await this.loadData();
            this.ngbDiseasesTargetsPanelService.setFieldList();
        }
        this.gridOptions.columnDefs = this.getTargetsTableGridColumns();
    }

    getTargetsTableGridColumns() {
        const headerCells = require('./ngbDiseasesTargetsTable_header.tpl.html');
        const colorCell = require('./ngbDiseasesTargetsTable_colorCell.tpl.html');
        const homologueCell = require('./ngbDIseasesTargetsTable_homologuesCell.tpl.html');

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
                enableFiltering: true,
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
                    };
                    break;
                case 'target name':
                    columnSettings = {
                        ...columnSettings,
                        minWidth: 200
                    };
                    break;
                case 'homologues':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: homologueCell,
                        enableSorting: false,
                        enableColumnMenu: false,
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

    showOthers(cell, event) {
        event.stopPropagation();
        cell.limit = 100000;
    }

    showLess(cell, event) {
        event.stopPropagation();
        cell.limit = 2;
    }

    async loadData () {
        this.loadingData = true;
        this.gridOptions.data = await this.ngbDiseasesTargetsPanelService.getTargetsResults()
            .then(success => {
                if (success) {
                    return this.targetsResults;
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

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.currentPage = 1;
        await this.loadData();
        this.$timeout(() => this.$scope.$apply());
    }

    async diseaseChanged() {
        this.resetDrugsData();
        this.resetSorting();
        await this.initialize();
        this.$timeout(() => this.$scope.$apply());
    }

    resetDrugsData() {
        this.ngbDiseasesTargetsPanelService.resetData();
        this.dispatcher.emit('target:diseases:targets:filters:reset');
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

    exportResults() {
        this.loadingData = true;
        this.ngbDiseasesTargetsPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.diseaseName}-targets.csv`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.loadingData = false;
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                    this.loadingData = false;
                }
                this.$timeout(() => this.$scope.$apply());
            });
    }
}
