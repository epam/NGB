export default class ngbTargetGenesFilterRangeController {

    prevValueFrom;
    prevValueTo;
    valueFrom;
    valueTo;

    static get UID() {
        return 'ngbTargetGenesFilterRangeController';
    }

    constructor($scope, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService) {
        Object.assign(this, {$scope, dispatcher, ngbTargetGenesTableService, ngbTargetsFormService});

        const range = [0, 100];
        this.prevValueFrom = this.valueFrom = isNaN(range[0]) || range[0] === null ? null : range[0];
        this.prevValueTo = this.valueTo = isNaN(range[1]) || range[1] === null ? null : range[1];
    }

    apply() {
        let shouldUpdate = false;
        const range = [this.prevValueFrom, this.prevValueTo];
        if (this.prevValueFrom !== this.valueFrom) {
            if (!this.valueFrom || !this.valueFrom.length) {
                this.valueFrom = null;
            }
            this.prevValueFrom = this.valueFrom;
            shouldUpdate = true;
            range[0] = this.valueFrom;
        }
        if (this.prevValueTo !== this.valueTo) {
            if (!this.valueTo || !this.valueTo.length) {
                this.valueTo = null;
            }
            this.prevValueTo = this.valueTo;
            shouldUpdate = true;
            range[1] = this.valueTo;
        }
        if (shouldUpdate) {
            if (this.ngbTargetsFormService.needSaveGeneChanges()) {
                this.dispatcher.emit('target:form:confirm:filter');
            } else {
                this.ngbTargetGenesTableService.setFilter(this.column.field, '');
                this.dispatcher.emit('target:form:filters:changed');
            }
        }
    }

    convertToNumber(value) {
        if (value !== null && value !== undefined) {
            if (typeof value !== 'string') {
                return value;
            } else if (isNaN(value.replace(',', '.'))) {
                return null;
            } else {
                return +(value.replace(',', '.'));
            }
        }
        return null;
    }
}
