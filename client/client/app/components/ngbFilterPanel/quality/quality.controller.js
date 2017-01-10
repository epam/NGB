import baseFilterController from '../baseFilterController';


export default class qualityController extends baseFilterController {
    static get UID() {
        return 'qualityController';
    }

    /** @ngInject */
    constructor(dispatcher, projectContext, $scope) {
        super(dispatcher, projectContext, $scope);

    }

    toggleQuality() {

        if (this.projectContext.vcfFilter.quality.from === null &&
            this.projectContext.vcfFilter.quality.to === null) {
            this.projectContext.vcfFilter.quality.value = [];
        } else {
            this.projectContext.vcfFilter.quality.value = [
                this.projectContext.vcfFilter.quality.from,
                this.projectContext.vcfFilter.quality.to
            ];
        }
        this.emitEvent();
    }

}
