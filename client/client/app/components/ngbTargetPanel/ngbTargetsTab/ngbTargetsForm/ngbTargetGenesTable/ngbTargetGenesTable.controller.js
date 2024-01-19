const PRIORITY_LIST = [{
    name: 'Low',
    value: 'LOW'
}, {
    name: 'High',
    value: 'HIGH'
}];

export default class ngbTargetGenesTableController {

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

    get priorityList() {
        return PRIORITY_LIST;
    }

    static get UID() {
        return 'ngbTargetGenesTableController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        $mdDialog,
        ngbTargetGenesTableService,
        ngbTargetsTabService,
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            $mdDialog,
            ngbTargetGenesTableService,
            ngbTargetsTabService
        });
        const initialize = this.initialize.bind(this);
        const getDataOnPage = this.getDataOnPage.bind(this);
        const getDataOnLastPage = this.getDataOnLastPage.bind(this);
        const refreshColumns = this.refreshColumns.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:form:gene:added', initialize);
        dispatcher.on('target:form:refreshed', getDataOnPage);
        dispatcher.on('target:form:add:gene', getDataOnLastPage);
        dispatcher.on('target:genes:columns:changed', initialize);
        dispatcher.on('target:form:filters:display:changed', refreshColumns);
        dispatcher.on('target:form:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:gene:added', initialize);
            dispatcher.removeListener('target:form:refreshed', getDataOnPage);
            dispatcher.removeListener('target:form:add:gene', getDataOnLastPage);
            dispatcher.removeListener('target:genes:columns:changed', initialize);
            dispatcher.removeListener('target:form:filters:display:changed', refreshColumns);
            dispatcher.removeListener('target:form:filters:changed', filterChanged);
        });
    }

    set loading(value) {
        this.ngbTargetsTabService.formLoading = value;
    }
    get targetModel() {
        return this.ngbTargetsTabService.targetModel;
    }
    get updateForce() {
        return this.ngbTargetsTabService.updateForce;
    }
    set updateForce(value) {
        this.ngbTargetsTabService.updateForce = value;
    }
    get totalPages() {
        return this.ngbTargetGenesTableService.totalPages;
    }
    get currentPage() {
        return this.ngbTargetGenesTableService.currentPage;
    }
    set currentPage(value) {
        this.ngbTargetGenesTableService.currentPage = value;
    }
    get tableResults() {
        return this.ngbTargetGenesTableService.tableResults;
    }
    get sortInfo() {
        return this.ngbTargetGenesTableService.sortInfo;
    }
    set sortInfo(value) {
        this.ngbTargetGenesTableService.sortInfo = value;
    }
    get displayFilters() {
        return this.ngbTargetGenesTableService.displayFilters;
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
        (this.initialize)();
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        if (this.tableResults) {
            this.gridOptions.data = this.tableResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getTableColumns();
    }

    refreshColumns() {
        this.gridOptions.columnDefs = this.getTableColumns();
        this.$timeout(() => this.$scope.$apply());
    }

    getTableColumns() {
        const headerCells = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_header.tpl.html');
        const inputCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_inputCell.tpl.html');
        const selectCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_selectCell.tpl.html');
        const listCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_listCell.tpl.html');
        const removeCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_removeCell.tpl.html');

        const result = [];
        const columnsList = this.ngbTargetGenesTableService.currentColumnFields;
        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            const settings = {
                name: column,
                displayName: this.ngbTargetGenesTableService.getColumnName(column),
                enableHiding: false,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            }
            const defaultSettings = {
                ...settings,
                enableColumnMenu: false,
                enableSorting: false,
                enableFiltering: false,
            };
            const parasiteSettings = {
                ...settings,
                enableColumnMenu: true,
                enableSorting: true,
                enableFiltering: this.displayFilters,
            };
            switch (column) {
                case 'geneId':
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: inputCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: inputCell,
                        };
                    }
                    break;
                case 'geneName':
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: listCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: listCell,
                        };
                    }
                    break;
                case 'taxId':
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: inputCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: inputCell,
                        };
                    }
                    break;
                case 'speciesName':
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: inputCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: inputCell,
                        };
                    }
                    break;
                case 'priority':
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: selectCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: selectCell,
                        };
                    }
                    break;
                case 'remove':
                    columnSettings = {
                        ...defaultSettings,
                        minWidth: 38,
                        maxWidth: 38,
                        cellTemplate: removeCell,
                        enableColumnMenu: false,
                        enableSorting: false,
                        enableFiltering: false,
                    };
                    break;
                default:
                    if (this.ngbTargetsTabService.isParasite) {
                        columnSettings = {
                            ...parasiteSettings
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings
                        };
                    }
                    break;
            }
            if (columnSettings) {
                result.push(columnSettings);
            }
        }
        return result;
    }

    async loadData () {
        this.loading = true;
        this.gridOptions.data = [];
        this.gridOptions.data = await this.ngbTargetGenesTableService.getTableResults()
            .then(success => {
                if (success) {
                    return this.tableResults;
                }
                return [];
            });
        this.$timeout(() => {
            this.$scope.$apply();
            this.dispatcher.emit('target:form:genes:results:updated');
        });
    }

    onSelectGene(row, gene) {
        this.ngbTargetsTabService.selectedGeneChanged(row, gene)
    }

    onChangeGeneName(row, field, text) {
        this.ngbTargetsTabService.setGeneModel(row, field, text);
    }

    savedIdentificationGene(index) {
        if (!this.targetModel.identifications || !this.targetModel.identifications.length) return false;
        const geneId = this.targetModel.genes[index].geneId;
        return this.targetModel.identifications.some(identification => {
            const {genesOfInterest, translationalGenes} = identification;
            return genesOfInterest.includes(geneId) || translationalGenes.includes(geneId);
        })
    }

    onClickRemove(event, row) {
        event.stopPropagation();
        const geneIndex = this.targetModel.genes.indexOf(row);
        if (!this.savedIdentificationGene(geneIndex)) {
            this.targetModel.genes.splice(geneIndex, 1);
        } else {
            this.openConfirmDialog(geneIndex);
        }
    }

    openConfirmDialog (index) {
        const gene = this.targetModel.genes[index];
        this.$mdDialog.show({
            template: require('./ngbGeneDeleteDlg.tpl.html'),
            controller: function($scope, $mdDialog, dispatcher) {
                $scope.geneName = gene.geneName;

                $scope.delete = function () {
                    dispatcher.emit('target:gene:delete');
                    $mdDialog.hide();
                };
                $scope.cancel = function () {
                    $mdDialog.hide();
                };
            },
        });

        this.dispatcher.on('target:gene:delete', () => {
            this.updateForce = true;
            this.targetModel.genes.splice(index, 1);
        });
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        this.currentPage = page;
        await this.loadData();
    }

    async getDataOnLastPage() {
        if (this.totalPages) {
            if (this.currentPage !== this.totalPages) {
                await this.getDataOnPage(this.totalPages);
            }
            this.$timeout(() => this.ngbTargetsTabService.addNewGene(true));
        }
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

    async filterChanged() {
        if (!this.gridApi) {
            return;
        }
        this.loadingData = true;
        this.currentPage = 1;
        await this.loadData();
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
}
