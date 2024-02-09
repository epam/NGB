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
        saveWidths: false,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: false,
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
        ngbTargetsFormService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            $mdDialog,
            ngbTargetGenesTableService,
            ngbTargetsFormService
        });
        const initialize = this.initialize.bind(this);
        const reloadCurrentPage = this.reloadCurrentPage.bind(this);
        const getDataOnLastPage = this.getDataOnLastPage.bind(this);
        const refreshColumns = this.refreshColumns.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        const resetSorting = this.resetSorting.bind(this);
        const confirmRestoreDialog = this.confirmRestoreDialog.bind(this);
        const confirmFilterDialog = this.confirmFilterDialog.bind(this);
        dispatcher.on('target:form:gene:added', initialize);
        dispatcher.on('target:form:saved', reloadCurrentPage);
        dispatcher.on('target:form:add:gene', getDataOnLastPage);
        dispatcher.on('target:genes:columns:changed', initialize);
        dispatcher.on('target:form:filters:display:changed', refreshColumns);
        dispatcher.on('target:form:filters:changed', filterChanged);
        dispatcher.on('target:form:sort:reset', resetSorting);
        dispatcher.on('target:form:restore:view', confirmRestoreDialog);
        dispatcher.on('target:form:confirm:filter', confirmFilterDialog);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:gene:added', initialize);
            dispatcher.removeListener('target:form:saved', reloadCurrentPage);
            dispatcher.removeListener('target:form:add:gene', getDataOnLastPage);
            dispatcher.removeListener('target:genes:columns:changed', initialize);
            dispatcher.removeListener('target:form:filters:display:changed', refreshColumns);
            dispatcher.removeListener('target:form:filters:changed', filterChanged);
            dispatcher.removeListener('target:form:sort:reset', resetSorting);
            dispatcher.removeListener('target:form:restore:view', confirmRestoreDialog);
            dispatcher.removeListener('target:form:confirm:filter', confirmFilterDialog);
        });
    }

    get loading() {
        return this.ngbTargetsFormService.loading;
    }
    set loading(value) {
        this.ngbTargetsFormService.loading = value;
    }
    get targetModel() {
        return this.ngbTargetsFormService.targetModel;
    }
    get updateForce() {
        return this.ngbTargetsFormService.updateForce;
    }
    set updateForce(value) {
        this.ngbTargetsFormService.updateForce = value;
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
    get isParasite() {
        return this.targetModel.type === this.ngbTargetsFormService.targetType.PARASITE;
    }
    get addedGenes() {
        return this.ngbTargetsFormService.addedGenes;
    }
    set addedGenes(value) {
        this.ngbTargetsFormService.addedGenes = value;
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
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.onColumnMoved.bind(this));
            }
        });
        (this.initialize)();
    }

    get removeColumnName() {
        const {columnField, removeColumn} = this.ngbTargetGenesTableService;
        return columnField[removeColumn];
    }

    onColumnMoved(movedColumn) {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        let orderedColumns = columns.map(c => c.name);
        if (movedColumn.name !== this.removeColumnName) {
            orderedColumns = orderedColumns.filter(c => c !== this.removeColumnName);
        }
        localStorage.setItem('targetGenesColumnsOrder', JSON.stringify(orderedColumns));
    }

    async initialize() {
        if (!this.gridOptions) {
            return;
        }
        if (this.tableResults) {
            this.gridOptions.data = this.tableResults;
        } else {
            await this.ngbTargetGenesTableService.initAdditionalColumns();
            await this.loadData();
        }
        this.saveSortConfiguration();
        this.gridOptions.columnDefs = this.getTableColumns();
        this.$timeout(() => this.sortColumns());
    }

    async reloadCurrentPage() {
        await this.ngbTargetGenesTableService.initAdditionalColumns();
        await this.loadData();
        this.gridOptions.columnDefs = this.getTableColumns();
    }

    refreshColumns() {
        this.gridOptions.columnDefs = this.getTableColumns();
        this.$timeout(() => this.$scope.$apply());
    }

    sortColumns() {
        if (!this.gridApi) return;
        const ordered = JSON.parse(localStorage.getItem('targetGenesColumnsOrder'));
        if (ordered && ordered.length) {
            this.gridApi.grid.columns.sort((c2, c1) => {
                if (!ordered.includes(this.removeColumnName)) {
                    if (c2.name === this.removeColumnName) return 1;
                    if (c1.name === this.removeColumnName) return -1;
                }
                if (ordered.includes(c2.name) && ordered.includes(c1.name)) {
                    return ordered.indexOf(c2.name) < ordered.indexOf(c1.name) ? -1 : 1;
                } else if (ordered.includes(c2.name) || ordered.includes(c1.name)) {
                    if (ordered.includes(c2.name)) return -1;
                    if (ordered.includes(c1.name)) return 1;
                }
                return 0;
            })
        }
    }

    getTableColumns() {
        const headerCells = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_header.tpl.html');
        const inputCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_inputCell.tpl.html');
        const selectCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_selectCell.tpl.html');
        const listCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_listCell.tpl.html');
        const removeCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_removeCell.tpl.html');
        const additionalCell = require('./ngbTargetGenesTableCells/ngbTargetGenesTable_additionalCell.tpl.html');

        const result = [];
        const columnsList = this.ngbTargetGenesTableService.currentColumnFields;

        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            const column = columnsList[i];
            const columnName = this.ngbTargetGenesTableService.getColumnName(column);
            const isColumnSort = this.ngbTargetGenesTableService.getIsColumnSort(columnName);
            const settings = {
                name: column,
                displayName: columnName,
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
                enableSorting: isColumnSort,
                enableFiltering: this.displayFilters,
            };
            switch (column) {
                case 'geneId':
                    if (this.isParasite) {
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
                    if (this.isParasite) {
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
                    if (this.isParasite) {
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
                    if (this.isParasite) {
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
                    if (this.isParasite) {
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
                        enableMove: false,
                    };
                    break;
                default:
                    if (this.isParasite) {
                        columnSettings = {
                            ...parasiteSettings,
                            cellTemplate: additionalCell,
                        };
                    } else {
                        columnSettings = {
                            ...defaultSettings,
                            cellTemplate: additionalCell,
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
        this.ngbTargetsFormService.selectedGeneChanged(row, gene)
    }

    onChangeGeneName(row, field, text) {
        this.ngbTargetsFormService.setGeneModel(row, field, text);
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
            this.deleteGeneFromTarget(geneIndex, row);
        } else {
            this.openConfirmDeleteGeneDialog(geneIndex);
        }
    }

    openConfirmDeleteGeneDialog (index) {
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
            this.deleteGeneFromTarget(index);
        });
    }

    deleteGeneFromTarget(index, row) {
        const updateTable = (i) => {
            this.gridOptions.data = [];
            this.targetModel.genes.splice(i, 1);
            this.$timeout(() => {
                this.$scope.$apply();
                this.gridOptions.data = this.tableResults;
                this.$timeout(() => this.$scope.$apply());
            });
        }

        if (this.isParasite) {
            if (index === -1) {
                index = this.addedGenes.indexOf(row);
                this.addedGenes.splice(index, 1);
                this.gridOptions.data = this.tableResults;
            } else {
                this.ngbTargetsFormService.removedGenes.push({...this.targetModel.genes[index]});
                updateTable(index);
            }
        } else {
            updateTable(index);
        }
    }

    async getDataOnPage(page) {
        if (!this.gridApi) {
            return;
        }
        if (this.ngbTargetsFormService.needSaveGeneChanges()) {
            const saveCallback = this.saveChangesCallBack();
            const cancelCallback = async () => {
                this.ngbTargetsFormService.addedGenes = [];
                this.ngbTargetsFormService.removedGenes = [];
                this.currentPage = page;
                await this.loadData();
            };
            this.openConfirmDialog(saveCallback, cancelCallback);
        } else {
            this.currentPage = page;
            await this.loadData();
        }
    }

    async getDataOnLastPage() {
        if (this.totalPages) {
            if (this.currentPage !== this.totalPages) {
                if (this.ngbTargetsFormService.needSaveGeneChanges()) {
                    const saveCallback = () => {
                        if (!this.ngbTargetsFormService.areGenesEmpty() &&
                            !this.ngbTargetsFormService.isSomeGeneEmpty()
                        ) {
                            this.currentPage = this.totalPages;
                            this.dispatcher.emit('target:form:changes:save', true);
                        }
                    };
                    const cancelCallback = async () => {
                        this.ngbTargetsFormService.removedGenes = [];
                        this.currentPage = this.totalPages;
                        await this.loadData();
                    };
                    this.openConfirmDialog(saveCallback, cancelCallback);
                } else {
                    if (!this.gridApi) return;
                    this.currentPage = this.totalPages;
                    await this.loadData();
                    this.$timeout(() => this.ngbTargetsFormService.addNewGene());
                }
            } else {
                this.$timeout(() => this.ngbTargetsFormService.addNewGene());
            }
        }
    }

    saveSortConfiguration() {
        this.savedSortConfiguration = this.gridApi.grid.columns
            .filter(column => !!column.sort)
            .map((column, priority) => ({
                field: column.field,
                sort: ({
                    ...column.sort,
                    priority
                })
        }));
    }

    setSortFromConfiguration() {
        if (!this.gridApi) return;
        const {columns = []} = this.gridApi.grid || {};
        columns.forEach(columnDef => {
            const [sortingConfig] = this.savedSortConfiguration
                .filter(c => c.field === columnDef.field);
            if (sortingConfig) {
                columnDef.sort = sortingConfig.sort;
            } else {
                columnDef.sort = {};
            }
        });
        this.saveSortConfiguration();
    }

    async sortChanged(grid, sortColumns) {
        if (this.ngbTargetsFormService.needSaveGeneChanges()) {
            const saveCallback = () => {
                if (this.ngbTargetsFormService.areGenesEmpty() ||
                    this.ngbTargetsFormService.isSomeGeneEmpty()
                ) {
                    this.setSortFromConfiguration();
                } else {
                    this.setSortFromConfiguration();
                    this.dispatcher.emit('target:form:changes:save');
                }
            };
            const cancelCallback = () => {
                this.ngbTargetsFormService.addedGenes = [];
                this.ngbTargetsFormService.removedGenes = [];
                this.sortChangeConfirmed(grid, sortColumns);
            };
            this.openConfirmDialog(saveCallback, cancelCallback);
        } else {
            await this.sortChangeConfirmed(grid, sortColumns);
        }
    }

    toggleMenu(event, toggleMenuGrid) {
        if (this.ngbTargetsFormService.needSaveGeneChanges()) {
            const saveCallback = () => {};
            const cancelCallback = () => {
                this.ngbTargetsFormService.addedGenes = [];
                this.ngbTargetsFormService.removedGenes = [];
                this.ngbTargetGenesTableService.resetTableResults();
                this.initialize();
                this.$timeout(() => this.$scope.$apply());
                toggleMenuGrid(event);
            };
            this.openConfirmDialog(saveCallback, cancelCallback);
        } else {
            toggleMenuGrid(event);
        }
    }

    async sortChangeConfirmed(grid, sortColumns) {
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
        this.saveSortConfiguration();
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

    confirmRestoreDialog() {
        const saveCallback = this.saveChangesCallBack();
        const cancelCallback = async () => {
            this.ngbTargetsFormService.addedGenes = [];
            this.ngbTargetsFormService.removedGenes = [];
            this.ngbTargetGenesTableService.resetTableResults();
            this.ngbTargetGenesTableService.restoreView();
            this.dispatcher.emit('target:form:reset:columns');
            this.refreshColumns();
        };
        this.openConfirmDialog(saveCallback, cancelCallback);
    }

    confirmFilterDialog(callback) {
        const saveCallback = () => {
            if (!this.ngbTargetsFormService.areGenesEmpty() &&
                !this.ngbTargetsFormService.isSomeGeneEmpty()
            ) {
                callback.save();
                this.dispatcher.emit('target:form:changes:save');
            }
        };
        const cancelCallback = async () => {
            this.ngbTargetsFormService.addedGenes = [];
            this.ngbTargetsFormService.removedGenes = [];
            callback.cancel();
            await this.loadData();
        };
        this.openConfirmDialog(saveCallback, cancelCallback);
    }

    saveChangesCallBack() {
        const saveCallback = () => {
            if (!this.ngbTargetsFormService.areGenesEmpty() &&
                !this.ngbTargetsFormService.isSomeGeneEmpty()
            ) {
                this.dispatcher.emit('target:form:changes:save');
            }
        };
        return saveCallback;
    }

    openConfirmDialog(saveCallback, cancelCallback) {
        this.$mdDialog.show({
            template: require('./ngbConfirmChangesDlg.tpl.html'),
            controller: function($scope, $mdDialog) {
                $scope.save = async function () {
                    saveCallback();
                    $mdDialog.hide();
                };
                $scope.cancel = async function () {
                    cancelCallback();
                    $mdDialog.cancel();
                };
            }
        });
    }
}
