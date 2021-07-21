export default class ngbGenesFilterRangeController {
    placeholderFrom;
    placeholderTo;
    projectContext;
    prevValueFrom;
    valueFrom;
    prevValueTo;
    valueTo;

    constructor(ngbGenesTableService) {
        this.ngbGenesTableService = ngbGenesTableService;
        const range = this.ngbGenesTableService.genesFilter[this.field.field] || [null, null];
        this.prevValueFrom = this.valueFrom = range[0] || null;
        this.prevValueTo = this.valueTo = range[1] || null;
    }

    static get UID() {
        return 'ngbGenesFilterRangeController';
    }

    static convertToNumber(value) {
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

    apply() {
        if (!this.ngbGenesTableService.canScheduleFilterGenes()) {
            return;
        }
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
            const isDefault = range[0] === null && range[1] === null;
            range[0] = ngbGenesFilterRangeController.convertToNumber(range[0]);
            range[1] = ngbGenesFilterRangeController.convertToNumber(range[1]);
            this.ngbGenesTableService.genesFilter[this.field.field] = isDefault ? undefined : range;
            this.ngbGenesTableService.scheduleFilterGenes();
        }
    }
}
