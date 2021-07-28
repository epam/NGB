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
        this.dispatcher.emitGlobalEvent(`feature:info:select:${db}`, {db, featureId: this.geneId});
    }

    get isInfoBeingEdited() {
        this.$scope.$parent.disableCloseBtn = this.ngbFeatureInfoPanelService.isInfoBeingEdited;
        return this.ngbFeatureInfoPanelService.isInfoBeingEdited;
    }

    get hasInfoHistory() {
        return this.ngbFeatureInfoPanelService.hasInfoHistory;
    }
}
