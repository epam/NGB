export default class ngbCoverageFilterRangeController {
    prevValueFrom;
    valueFrom;
    prevValueTo;
    valueTo;

    get filterInfo() {
        return this.ngbCoveragePanelService.filterInfo || {};
    }

    static get UID() {
        return 'ngbCoverageFilterRangeController';
    }

    constructor(dispatcher, ngbCoveragePanelService) {
        Object.assign(this, {dispatcher, ngbCoveragePanelService});
        const range = this.filterInfo[this.field.field] || {from: null, to: null};
        this.prevValueFrom = this.valueFrom = isNaN(range.from) || range.from;
        this.prevValueTo = this.valueTo = isNaN(range.to) || range.to;
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
        let shouldUpdate = false;
        const range = {
            from: this.prevValueFrom,
            to: this.prevValueTo
        };
        if (this.prevValueFrom !== this.valueFrom) {
            if (!this.valueFrom || !this.valueFrom.length) {
                this.valueFrom = null;
            }
            this.prevValueFrom = this.valueFrom;
            shouldUpdate = true;
            range.from = this.valueFrom;
        }
        if (this.prevValueTo !== this.valueTo) {
            if (!this.valueTo || !this.valueTo.length) {
                this.valueTo = null;
            }
            this.prevValueTo = this.valueTo;
            shouldUpdate = true;
            range.to = this.valueTo;
        }
        if (shouldUpdate) {
            range.from = ngbCoverageFilterRangeController.convertToNumber(range.from);
            range.to = ngbCoverageFilterRangeController.convertToNumber(range.to);
            this.ngbCoveragePanelService.setFilter(this.field.field, range);
            this.dispatcher.emit('coverage:filter:changed');
        }
    }
}
