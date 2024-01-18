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
        enableHorizontalScrollbar: true,
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
        const getDataOnLastPage = this.getDataOnLastPage.bind(this);
        const initialize = this.initialize.bind(this);
        const getDataOnPage = this.getDataOnPage.bind(this);
        dispatcher.on('target:form:add:gene', getDataOnLastPage);
        dispatcher.on('target:form:gene:added', initialize);
        dispatcher.on('target:form:refreshed', getDataOnPage);
        dispatcher.on('target:genes:columns:changed', initialize);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:add:gene', getDataOnLastPage);
            dispatcher.removeListener('target:form:gene:added', initialize);
            dispatcher.removeListener('target:form:refreshed', getDataOnPage);
            dispatcher.removeListener('target:genes:columns:changed', initialize);
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

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: [],
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
        if (this.tableResults) {
            this.gridOptions.data = this.tableResults;
        } else {
            await this.loadData();
        }
        this.gridOptions.columnDefs = this.getTableColumns();
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
            columnSettings = {
                name: column,
                displayName: this.ngbTargetGenesTableService.getColumnName(column),
                enableHiding: false,
                enableColumnMenu: false,
                enableSorting: false,
                enableFiltering: false,
                field: column,
                headerTooltip: column,
                headerCellTemplate: headerCells,
                minWidth: 40,
                width: '*'
            };
            switch (column) {
                case 'geneId':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: inputCell,
                    };
                    break;
                case 'geneName':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: listCell,
                    };
                    break;
                case 'taxId':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: inputCell,
                    };
                    break;
                case 'speciesName':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: inputCell,
                    };
                    break;
                case 'priority':
                    columnSettings = {
                        ...columnSettings,
                        cellTemplate: selectCell,
                    };
                    break;
                case 'remove':
                    columnSettings = {
                        ...columnSettings,
                        minWidth: 38,
                        maxWidth: 38,
                        cellTemplate: removeCell,
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
        this.loading = true;
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
        this.gridOptions.data = [];
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
}
