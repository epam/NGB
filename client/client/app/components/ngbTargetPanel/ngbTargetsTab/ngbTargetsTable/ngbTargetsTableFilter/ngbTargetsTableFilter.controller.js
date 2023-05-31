export default class ngbTargetsTableFilterController {

    prevValue;
    value;

    get filterInfo() {
        return this.ngbTargetsTableService.filterInfo || {};
    }

    static get UID() {
        return 'ngbTargetsTableFilterController';
    }

    constructor(dispatcher, ngbTargetsTableService) {
        Object.assign(this, {dispatcher, ngbTargetsTableService});
        this.prevValue = this.value = this.filterInfo[this.column.field];
    }

    apply() {
        let shouldUpdate = false;
        let string = this.prevValue;
        if (this.prevValue !== this.value) {
            if (!this.value || !this.value.length) {
                this.value = null;
            }
            this.prevValue = this.value;
            shouldUpdate = true;
            string = this.value;
        }
        if (shouldUpdate) {
            this.ngbTargetsTableService.setFilter(this.column.field, string);
            this.dispatcher.emit('targets:filter:changed');
        }
    }


}
