import  baseController from '../../../../shared/baseController';

export default class ngbGenesTableColumnController extends baseController {

    displayGenesFilter;
    events = {
        'ngbColumns:genes:change': ::this.onColumnChange,
        'genes:info:loaded': ::this.onColumnChange,
        'reference:change': ::this.loadColumns,
        'display:genes:filter' : ::this.updateDisplayGenesFilterValue
    };

    constructor(dispatcher, ngbGenesTableService, $scope, $timeout) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbGenesTableService
        });

        this.displayGenesFilter = this.ngbGenesTableService.displayGenesFilter;

        this.initEvents();
    }

    static get UID() {
        return 'ngbGenesTableColumnController';
    }

    $onInit() {
        this.loadColumns();
    }

    updateDisplayGenesFilterValue() {
        this.displayGenesFilter = this.ngbGenesTableService.displayGenesFilter;
    }

    onDisplayGenesFilterChange() {
        this.ngbGenesTableService.setDisplayGenesFilter(this.displayGenesFilter, false);
    }

    onGenesRestoreViewClick() {
        this.ngbGenesTableService.genesTableColumns = [];
        this.dispatcher.emit('genes:restore');
        this.columnsList.map(
            column => column.selection = this.ngbGenesTableService.genesTableColumns.includes(column.name));
        this.addColumnToTable();
    }

    loadColumns() {
        this.onColumnChange([]);
        this.$timeout(::this.$scope.$apply);
    }

    addColumnToTable() {
        const currentColumns = this.ngbGenesTableService.genesTableColumns;
        const currentOptionalColumns = this.ngbGenesTableService.optionalGenesColumns
            .filter(value => currentColumns.includes(value));
        const infoFields = this.columnsList
            .filter(column => column.selection === true)
            .map(m => m.name);
        const [added] = infoFields.filter(i => currentOptionalColumns.indexOf(i) === -1);
        const [removed] = currentOptionalColumns.filter(c => infoFields.indexOf(c) === -1);
        if (added) {
            if (currentColumns[currentColumns.length - 1] === 'info' || currentColumns[currentColumns.length - 1] === 'molecularView') {
                if (currentColumns[currentColumns.length - 2] === 'info' || currentColumns[currentColumns.length - 2] === 'molecularView') {
                    currentColumns.splice(currentColumns.length - 2, 0, added);
                } else {
                    currentColumns.splice(currentColumns.length - 1, 0, added);
                }
            } else {
                currentColumns.push(added);
            }
            this.ngbGenesTableService.genesTableColumns = currentColumns;
        }
        if (removed) {
            const index = currentColumns.indexOf(removed);
            if (~index) {
                currentColumns.splice(index, 1);
                this.ngbGenesTableService.genesTableColumns = currentColumns;
            }
        }
        this.dispatcher.emit('ngbColumns:genes:change');
        this.dispatcher.emit('genes:refresh');
    }

    onColumnChange() {
        this.columnsList = [];
        this.ngbGenesTableService.optionalGenesColumns.forEach(c => this.columnsList.push({name: c}));
        const infoFields = this.ngbGenesTableService.genesTableColumns;
        this.columnsList.forEach(m => m.selection = infoFields.indexOf(m.name) >= 0);
    }
}
