export default class ngbTargetsFormActionsController {

    allColumns = [];

    get columnsList() {
        return this.allColumns.filter(c => (
            !this.ngbTargetGenesTableService.defaultColumns.includes(c.name)
        ));
    }

    static get UID() {
        return 'ngbTargetsFormActionsController';
    }

    constructor(dispatcher, ngbTargetGenesTableService, ngbTargetsFormService) {
        Object.assign(this, {dispatcher, ngbTargetGenesTableService, ngbTargetsFormService});
    }

    $onInit() {
        this.setColumnsList();
    }

    async setColumnsList() {
        await this.ngbTargetsFormService.setColumnsList()
            .then(columns => {
                this.allColumns = columns.map(c => {
                    const column = { name: c };
                    if (this.ngbTargetGenesTableService.currentColumns.includes(c)) {
                        column.selection = true;
                    }
                    return column;
                });
            })
    }

    get displayFilters() {
        return this.ngbTargetGenesTableService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbTargetGenesTableService.displayFilters = value;
    }

    async onChangeShowFilters() {
        await this.ngbTargetGenesTableService.onChangeShowFilters();
    }

    onClickRestore() {
        this.ngbTargetGenesTableService.additionalColumns = [];
        this.dispatcher.emit('target:genes:columns:changed');
    }

    onChangeColumn() {
        const currentSelectedColumns = this.ngbTargetGenesTableService.additionalColumns;
        const newSelectedColumns = this.columnsList
            .filter(column => column.selection === true)
            .map(m => m.name);
        const [added] = newSelectedColumns.filter(i => currentSelectedColumns.indexOf(i) === -1);
        const [removed] = currentSelectedColumns.filter(c => newSelectedColumns.indexOf(c) === -1);
        if (added) {
            const columns = this.ngbTargetGenesTableService.additionalColumns;
            columns.push(added)
            this.ngbTargetGenesTableService.additionalColumns = columns;
        }
        if (removed) {
            const columns = this.ngbTargetGenesTableService.additionalColumns;
            const index = columns.indexOf(removed);
            if (index >= 0) {
                columns.splice(index, 1);
                this.ngbTargetGenesTableService.additionalColumns = columns;
            }
        }
        this.dispatcher.emit('target:genes:columns:changed');
    }
}
