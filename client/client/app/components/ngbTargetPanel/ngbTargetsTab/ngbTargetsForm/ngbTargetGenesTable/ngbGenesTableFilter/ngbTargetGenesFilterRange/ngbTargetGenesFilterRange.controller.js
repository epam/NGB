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

        const range = [null, null];
        this.prevValueFrom = this.valueFrom = isNaN(range[0]) || range[0] === null ? null : range[0];
        this.prevValueTo = this.valueTo = isNaN(range[1]) || range[1] === null ? null : range[1];
    }

    onBlur() {
        if (this.applying) {
            return;
        }
        this.apply();
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.applying = true;
                this.apply();
                break;
            default:
                break;
        }
    }

    apply() {
        let shouldUpdate = false;
        if (this.prevValueFrom !== this.valueFrom) {
            if (!this.valueFrom || !this.valueFrom.length) {
                this.valueFrom = null;
            }
            shouldUpdate = true;
        }
        if (this.prevValueTo !== this.valueTo) {
            if (!this.valueTo || !this.valueTo.length) {
                this.valueTo = null;
            }
            shouldUpdate = true;
        }
        if (shouldUpdate) {
            if (this.ngbTargetsFormService.needSaveGeneChanges()) {
                this.dispatcher.emit('target:form:confirm:filter', {
                    save: this.applyCanceled.bind(this),
                    cancel: this.applyConfirmed.bind(this)
                });
            } else {
                this.applyConfirmed();
            }
        }
    }

    applyCanceled() {
        this.applying = false;
        this.valueFrom = this.prevValueFrom;
        this.valueTo = this.prevValueTo;
    }

    applyConfirmed() {
        this.applying = false;
        if (this.prevValueFrom !== this.valueFrom) {
            this.prevValueFrom = this.valueFrom;
        }
        if (this.prevValueTo !== this.valueTo) {
            this.prevValueTo = this.valueTo;
        }
        this.ngbTargetGenesTableService.setFilter(this.column.field, [{
            from: this.valueFrom,
            to: this.valueTo
        }]);
        this.dispatcher.emit('target:form:filters:changed');
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
