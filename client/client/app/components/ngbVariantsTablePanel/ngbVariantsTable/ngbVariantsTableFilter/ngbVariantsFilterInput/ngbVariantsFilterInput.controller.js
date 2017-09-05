export default class ngbVariantsFilterInputController {
    static get UID() {
        return 'ngbVariantsFilterInputController';
    }

    projectContext;
    prevValue;
    value;

    constructor(projectContext) {
        this.projectContext = projectContext;
        this.prevValue = this.value = this.projectContext.vcfFilter.additionalFilters ?
            this.projectContext.vcfFilter.additionalFilters[this.field.field] : false;
    }

    apply() {
        if (!this.projectContext.canScheduleFilterVariants()) {
            return;
        }
        if (this.prevValue !== this.value) {
            this.prevValue = this.value;
            this.projectContext.vcfFilter.additionalFilters[this.field.field] = (this.value && this.value.length) ? this.value : undefined;
            this.projectContext.scheduleFilterVariants();
        }
    }
}