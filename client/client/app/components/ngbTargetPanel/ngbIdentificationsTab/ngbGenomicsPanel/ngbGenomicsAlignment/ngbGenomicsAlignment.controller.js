export default class ngbGenomicsAlignmentController {

    static get UID() {
        return 'ngbGenomicsAlignmentController';
    }

    constructor(ngbGenomicsPanelService) {
        Object.assign(this, {ngbGenomicsPanelService});
    }

    get alignment() {
        return this.ngbGenomicsPanelService.alignment;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {}
}
