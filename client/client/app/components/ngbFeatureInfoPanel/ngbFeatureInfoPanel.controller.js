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

    get isGeneInfoEdited() {
        this.$scope.$parent.disableCloseBtn = this.ngbFeatureInfoPanelService.isGeneInfoEdited;
        return this.ngbFeatureInfoPanelService.isGeneInfoEdited;
    }

    get isGeneInfoHistory() {
        return this.ngbFeatureInfoPanelService.isGeneInfoHistory;
    }
}
