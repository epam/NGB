export default class ngbIdentificationsTabController {

    isOpen = {
        drugs: false,
        description: false,
        sequences: false,
        genomics: false,
        structure: false,
        bibliography: false
    }

    static get UID() {
        return 'ngbIdentificationsTabController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbTargetPanelService});
        this.dispatcher.on('description:is:assigned', this.refresh.bind(this));
    }

    refresh() {
        this.$timeout(::this.$scope.$apply);
    }

    get identificationData () {
        return this.ngbTargetPanelService.identificationData;
    }

    get targetName() {
        const target = this.ngbTargetPanelService.identificationTarget;
        return target && target.target.name;
    }

    get interest() {
        const target = this.ngbTargetPanelService.identificationTarget;
        return target && target.interest.map(i => i.chip).join(', ');
    }

    get translational() {
        const target = this.ngbTargetPanelService.identificationTarget;
        return target && target.translational.map(i => i.chip).join(', ');
    }

    get descriptions() {
        return this.ngbTargetPanelService.descriptions;
    }

    get shortDescription() {
        return this.ngbTargetPanelService.shortDescription;
    }

    get diseasesCount() {
        return this.identificationData && this.identificationData.diseasesCount;
    }

    get knownDrugsCount() {
        return this.identificationData && this.identificationData.knownDrugsCount;
    }
}
