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
        if (this.ngbTargetGenesTableService.tableResults) {
            this.gridOptions.data = this.ngbTargetGenesTableService.tableResults;
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
        const columnsList = this.ngbTargetGenesTableService.getColumnList();
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
                    return this.ngbTargetGenesTableService.tableResults;
                }
                return [];
            });
        // this.dispatcher.emit('target:identification:drugs:results:updated');
        this.$timeout(() => this.$scope.$apply());
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
}
