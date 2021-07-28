export default class ngbGenesFilterInputController {
    prevValue;
    value;

    constructor(ngbGenesTableService) {
        this.ngbGenesTableService = ngbGenesTableService;
        this.column = this.ngbGenesTableService.getColumnOriginalName(this.field.field);
        this.prevValue = this.value = this.ngbGenesTableService.defaultGenesColumns.includes(this.column)
            ? this.ngbGenesTableService.genesFilter[this.column]
            : this.ngbGenesTableService.genesFilter.additionalFilters[this.column];
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
            if (this.ngbGenesTableService.defaultGenesColumns.includes(this.column)) {
                this.ngbGenesTableService.genesFilter[this.column]
                    = (this.value && this.value.length) ? this.value : undefined;
            } else {
                this.ngbGenesTableService.genesFilter.additionalFilters[this.column] =
                    (this.value && this.value.length) ? this.value : undefined;
            }
            this.ngbGenesTableService.scheduleFilterGenes();
        }
    }
}
