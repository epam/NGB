export default class ngbFeatureInfoPanelController {

    static get UID() {
        return 'ngbFeatureInfoPanelController';
    }

    get shouldDisplayGeneInfo() {
        return this.$scope.$ctrl.geneId;
    }


    constructor($scope, dispatcher, ngbFeatureInfoConstant, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbFeatureInfoConstant, ngbFeatureInfoPanelService});
    }

    selectTab(db){
        this.ngbFeatureInfoPanelService.isMainTabSelected = false;
        this.dispatcher.emitGlobalEvent(`feature:info:select:${db}`, {db, featureId: this.geneId});
    }

    get editMode() {
        const editMode = this.ngbFeatureInfoPanelService.editMode;
        this.$scope.$parent.editMode = editMode;
        return editMode;
    }

    selectMainTab() {
        this.ngbFeatureInfoPanelService.isMainTabSelected = true;
    }

    selectHistoryTab () {
        this.ngbFeatureInfoPanelService.isMainTabSelected = false;
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
}
