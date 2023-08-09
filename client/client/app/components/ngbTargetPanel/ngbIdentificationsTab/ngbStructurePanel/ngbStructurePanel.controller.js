export default class ngbStructurePanelController {

    static get UID() {
        return 'ngbStructurePanelController';
    }

    constructor(dispatcher, ngbStructurePanelService) {
        Object.assign(this, {dispatcher, ngbStructurePanelService});
    }

    get loading() {
        return this.ngbStructurePanelService.loading;
    }

    get sourceOptions () {
        return this.ngbStructurePanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbStructurePanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbStructurePanelService.sourceModel = value;
    }

    get pdbDescriptionLoading() {
        return this.ngbStructurePanelService.pdbDescriptionLoading;
    }
    get pdbDescriptionFailed() {
        return this.ngbStructurePanelService.pdbDescriptionFailed;
    }
    get pdbDescriptionErrorMessageList() {
        return this.ngbStructurePanelService.pdbDescriptionErrorMessageList;
    }

    onChangeSource() {
        this.dispatcher.emit('target:identification:structure:source:changed');
    }

    onFileUpload() {}
}
