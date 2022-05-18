export default class ngbFeatureInfoMainActionsController {

    static get UID() {
        return 'ngbFeatureInfoMainActionsController';
    }

    constructor(dispatcher, ngbFeatureInfoPanelService) {
        Object.assign(this, {dispatcher, ngbFeatureInfoPanelService});
        this.dispatcher.on('feature:info:changes:cancel', this.onClickCancelBtn.bind(this));
    }

    get editMode() {
        return this.ngbFeatureInfoPanelService.editMode;
    }

    get disableSave () {
        return this.ngbFeatureInfoPanelService.disableSaveButton();
    }

    get saveInProgress () {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }

    onClickEditBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:edit:click');
    }

    onClickCancelBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:cancel:click');
    }

    onClickSaveBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:save:click');
    }

    onClickAddBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:add:click', 'sequencePanel');
    }
}
