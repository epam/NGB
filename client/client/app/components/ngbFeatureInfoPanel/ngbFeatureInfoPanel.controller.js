export default class ngbFeatureInfoPanelController {

    static get UID() {
        return 'ngbFeatureInfoPanelController';
    }

    get shouldDisplayGeneInfo() {
        return this.$scope.$ctrl.geneId;
    }

    get isGeneralInfoOpen () {
        return this.ngbFeatureInfoPanelService.isGeneralInfoOpen;
    }

    constructor($scope, dispatcher, ngbFeatureInfoConstant, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbFeatureInfoConstant, ngbFeatureInfoPanelService});
        this.dispatcher.on('feature:info:changes:cancel', this.onClickCancelBtn.bind(this));
    }

    selectTab(db){
        this.dispatcher.emitGlobalEvent(`feature:info:select:${db}`, {db, featureId: this.geneId});
    }

    get editMode() {
        const editMode = this.ngbFeatureInfoPanelService.editMode;
        this.$scope.$parent.editMode = editMode;
        return editMode;
    }

    set editMode(value) {
        this.ngbFeatureInfoPanelService.editMode = value;
    }

    get disableSave () {
        return this.ngbFeatureInfoPanelService.disableSaveButton();
    }

    set saveInProgress(progress) {
        this.ngbFeatureInfoPanelService.saveInProgress = progress;
    }

    selectHistoryTab () {
        this.ngbFeatureInfoPanelService.getHistoryInProgress = true;
        this.ngbFeatureInfoPanelService.getGeneInfoHistory(this.fileId, this.uuid)
            .then((success) => {
                this.ngbFeatureInfoPanelService.getHistoryInProgress = false;
                if (success) {
                    return true;
                }
                return false;
            });
    }

    onClickEditBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.editMode = true;
        this.ngbFeatureInfoPanelService.newAttributes = this.properties;
    }

    onClickSaveBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.saveInProgress = true;
        this.ngbFeatureInfoPanelService.saveNewAttributes();
        this.properties = [...this.ngbFeatureInfoPanelService.newAttributes
            .map(newAttribute => (
                [
                    newAttribute.name,
                    newAttribute.value,
                    newAttribute.attribute,
                    newAttribute.deleted || false
                ]
            ))];
        this.feature = this.ngbFeatureInfoPanelService.updateFeatureInfo(this.feature);
        this.ngbFeatureInfoPanelService.sendNewGeneInfo(this.fileId, this.uuid, this.feature)
            .then((success) => {
                this.saveInProgress = false;
                const data = { trackId: this.fileId };
                if (success) {
                    this.onClickCancelBtn();
                    this.dispatcher.emitSimpleEvent('feature:info:saved', data);
                }
                this.$scope.$apply();
            });
    }

    onClickCancelBtn (event) {
        if (event) {
            event.stopPropagation();
        }
        this.editMode = false;
        this.ngbFeatureInfoPanelService.newAttributes = null;
        this.saveInProgress = false;
        this.ngbFeatureInfoPanelService.saveError = null;
    }
}
