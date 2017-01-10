import baseFilterController from '../baseFilterController';

export default class variantsController extends baseFilterController {
    static get UID() {
        return 'variantsController';
    }

    /** @ngInject */
    constructor(dispatcher, projectContext, $scope) {
        super(dispatcher, projectContext, $scope);

        this.INIT();
    }

    INIT() {
        this.vcfTypes = [{name: 'SNP', variationType: 'SNV'}, {name: 'BND', variationType: 'BND'},
            {name: 'Deletion', variationType: 'DEL'}, {name: 'Insertion', variationType: 'INS'},
            {name: 'Duplication', variationType: 'DUP'}, {name: 'Inversion', variationType: 'INV'}];
    }

    toggleVcfTypes(item) {
        const idx = this.projectContext.vcfFilter.selectedVcfTypes.indexOf(item.variationType);
        if (idx > -1) {
            this.projectContext.vcfFilter.selectedVcfTypes.splice(idx, 1);
        }
        else {
            this.projectContext.vcfFilter.selectedVcfTypes.push(item.variationType);
        }
        this.emitEvent();
    }

    existsVcfVariant(item) {
        if (this.projectContext &&
            this.projectContext.vcfFilter &&
            this.projectContext.vcfFilter.selectedVcfTypes) {
            return this.projectContext.vcfFilter.selectedVcfTypes.indexOf(item.variationType) > -1;
        }
        return false;
    }

}
