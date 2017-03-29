export default class ngbVariantsFilterCheckboxController {
    static get UID() {
        return 'ngbVariantsFilterCheckboxController';
    }

    projectContext;
    value = false;

    constructor(projectContext) {
        this.projectContext = projectContext;
        const name = this.field.field.toLowerCase();
        switch (name) {
            case 'exons': this.prevValue = this.value = this.projectContext.vcfFilter.exons; break;
            default: {
                this.prevValue = this.value = this.projectContext.vcfFilter.additionalFilters ?
                    this.projectContext.vcfFilter.additionalFilters[this.field.field] : false;
            } break;
        }
    }

    apply() {
        const name = this.field.field.toLowerCase();
        switch (name) {
            case 'exons': this.projectContext.vcfFilter.exons = this.value; break;
            default: {
                this.projectContext.vcfFilter.additionalFilters[this.field.field] = this.value ? this.value : undefined;
            } break;
        }
        this.projectContext.filterVariants();
    }
}