import ngbConstants from '../../../../../constants';

const COLUMN_LIST = ['target', 'drug', 'type', 'mechanism of action', 'action type', 'target name', 'phase', 'status', 'source'];

export default class ngbDiseasesDrugsPanelController {

    get columnList() {
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
        return 'ngbDiseasesDrugsPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesDrugsPanelService,
        ngbDiseasesTabService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesDrugsPanelService,
            ngbDiseasesTabService
        });
        const diseaseChanged = this.diseaseChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:diseases:disease:changed', diseaseChanged);
        dispatcher.on('target:diseases:drugs:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:disease:changed', diseaseChanged);
            dispatcher.removeListener('target:diseases:drugs:filters:changed', filterChanged);
        });
    }

    get loadingData() {
        return this.ngbDiseasesDrugsPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbDiseasesDrugsPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbDiseasesDrugsPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbDiseasesDrugsPanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbDiseasesDrugsPanelService.emptyResults;
    }
    get pageSize () {
        return this.ngbDiseasesDrugsPanelService.pageSize;
    }
    get totalPages() {
        return this.ngbDiseasesDrugsPanelService.totalPages;
    }
    get currentPage() {
        return this.ngbDiseasesDrugsPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbDiseasesDrugsPanelService.currentPage = value;
    }
    get sortInfo() {
        return this.ngbDiseasesDrugsPanelService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbDiseasesDrugsPanelService.sortInfo = value;
    }
    get filterInfo() {
        return this.ngbDiseasesDrugsPanelService.filterInfo;
    }
    set filterInfo(value) {
        this.ngbDiseasesDrugsPanelService.filterInfo = value;
    }
    get drugsResults() {
        return this.ngbDiseasesDrugsPanelService.drugsResults;
    }
    get diseaseName() {
        return (this.ngbDiseasesTabService.diseasesData || {}).name;
    }

    get tmapLoading() {
        return this.ngbDiseasesDrugsPanelService.tmapLoading;
    }
    set tmapLoading(value) {
        this.ngbDiseasesDrugsPanelService.tmapLoading = value;
    }
    get tmapFailed() {
        return this.ngbDiseasesDrugsPanelService.tmapFailed;
    }
    get tmapErrorList() {
        return this.ngbDiseasesDrugsPanelService.tmapErrorList;
    }
    get tmapUrl() {
        return this.ngbDiseasesDrugsPanelService.tmapUrl;
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
        if (this.drugsResults) {
            this.gridOptions.data = this.drugsResults;
        } else {
            await this.loadData();
            this.ngbDiseasesDrugsPanelService.setFieldList();
        }
        this.gridOptions.columnDefs = this.getDrugsTableGridColumns();
    }

    getDrugsTableGridColumns() {
        const headerCells = require('./ngbDiseasesDrugsTable_header.tpl.html');
        const linkCell = require('./ngbDiseasesDrugsTable_linkCell.tpl.html');

        const result = [];
        const columnsList = this.columnList;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            columnSettings = {
                name: column,
                enableHiding: false,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: true,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'drug':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
                    };
                    break;
                case 'source':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: linkCell
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
        this.gridOptions.data = await this.ngbDiseasesDrugsPanelService.getDrugsResults()
            .then(success => {
                if (success) {
                    return this.drugsResults;
                }
                return [];
            });
        this.dispatcher.emit('target:diseases:drugs:results:updated');
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

    onClickLink(row, event) {
        event.stopPropagation();
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
        if (this.$scope.gridApi) {
            this.$scope.gridApi.grid.modifyRows(this.$scope.gridOptions.data);
            this.$scope.gridApi.core.refresh();
        }
    }

    resetDrugsData() {
        this.ngbDiseasesDrugsPanelService.resetData();
        this.dispatcher.emit('target:diseases:drugs:filters:reset');
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
        this.ngbDiseasesDrugsPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.diseaseName}-drugs.csv`);

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

    async onClickTMAP() {
        if (!this.tmapUrl) {
            this.tmapLoading = true;
            await this.ngbDiseasesDrugsPanelService.generateTMAP();
            this.$timeout(() => {
                this.$scope.$apply();
                this.showTMAP();
            });
        } else {
            this.showTMAP();
        }
    }

    showTMAP() {
        let base = ngbConstants.urlPrefix || '';
        if (base && base.length) {
            if (!base.endsWith('/')) {
                base = base.concat('/');
            }
        }
        window.open(`${base}${this.tmapUrl}`, '_blank');
    }
}
