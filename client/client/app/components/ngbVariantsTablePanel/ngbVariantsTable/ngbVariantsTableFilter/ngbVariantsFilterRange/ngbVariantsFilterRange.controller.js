export default class ngbVariantsFilterRangeController {
    static get UID() {
        return 'ngbVariantsFilterRangeController';
    }

    placeholderFrom;
    placeholderTo;
    projectContext;
    prevValueFrom;
    valueFrom;
    prevValueTo;
    valueTo;

    constructor(projectContext) {
        this.projectContext = projectContext;
        let range;
        switch (this.field.field) {
            case 'startIndex': {
                this.placeholderFrom = 'From';
                this.placeholderTo = 'To';
                const startIndex = this.projectContext.vcfFilter.startIndex === undefined ? null : this.projectContext.vcfFilter.startIndex;
                const endIndex = this.projectContext.vcfFilter.endIndex === undefined ? null : this.projectContext.vcfFilter.endIndex;
                range = [startIndex, endIndex];
            } break;
            default: {
                range = this.projectContext.vcfFilter.additionalFilters ?
                    this.projectContext.vcfFilter.additionalFilters[this.field.field] : [null, null];
            } break;
        }
        range = range || [null, null];
        this.prevValueFrom = this.valueFrom = isNaN(range[0]) || range[0] === null ? null : range[0];
        this.prevValueTo = this.valueTo = isNaN(range[1]) || range[1] === null ? null : range[1];
    }

    apply() {
        if (!this.projectContext.canScheduleFilterVariants()) {
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
            const [info] = this.projectContext.vcfInfo.filter(i => i.name.toLowerCase() === this.field.field.toLowerCase());
            let type = 'string';
            if (info) {
                type = info.type.toLowerCase();
            }
            switch (type) {
                case 'integer':
                case 'float': {
                    range[0] = ngbVariantsFilterRangeController.convertToNumber(range[0]);
                    range[1] = ngbVariantsFilterRangeController.convertToNumber(range[1]);
                } break;
            }
            if (this.field.field === 'startIndex') {
                range[0] = ngbVariantsFilterRangeController.convertToNumber(range[0]);
                range[1] = ngbVariantsFilterRangeController.convertToNumber(range[1]);
                this.projectContext.vcfFilter.startIndex = range[0] === null ? undefined : range[0];
                this.projectContext.vcfFilter.endIndex = range[1] === null ? undefined : range[1];
            } else {
                this.projectContext.vcfFilter.additionalFilters[this.field.field] = isDefault ? undefined : range;
            }
            this.projectContext.scheduleFilterVariants();
        }
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
}
