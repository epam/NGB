export default class ngbGenesFilterInputController {
    prevValue;
    value;

    constructor(ngbGenesTableService) {
        this.ngbGenesTableService = ngbGenesTableService;
        this.prevValue = this.value = this.ngbGenesTableService.prefixedDefaultGenesColumns.includes(this.field.field)
            ? this.ngbGenesTableService.genesFilter[this.field.field]
            : this.ngbGenesTableService.genesFilter.additionalFilters[this.field.field];
    }

    static get UID() {
        return 'ngbGenesFilterInputController';
    }

    apply() {
        if (!this.ngbGenesTableService.canScheduleFilterGenes()) {
            return;
        }
        if (this.prevValue !== this.value) {
            this.prevValue = this.value;
            if (this.ngbGenesTableService.prefixedDefaultGenesColumns.includes(this.field.field)) {
                this.ngbGenesTableService.genesFilter[this.field.field]
                    = (this.value && this.value.length) ? this.value : undefined;
            } else {
                this.ngbGenesTableService.genesFilter.additionalFilters[this.field.field] =
                    (this.value && this.value.length) ? this.value : undefined;
            }
            this.ngbGenesTableService.scheduleFilterGenes();
        }
    }
}
