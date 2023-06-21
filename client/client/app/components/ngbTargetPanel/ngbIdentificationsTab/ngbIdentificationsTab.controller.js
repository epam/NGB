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

    constructor(dispatcher, ngbTargetPanelService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService});
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

    get description() {
        if (this.identificationData && this.identificationData.description) {
            return Object.values(this.identificationData.description)[0];
        }
        return 'description';
    }

    get shortDescription() {
        return `${this.description.substring(0, 150)}...`;
    }

    get diseasesCount() {
        return this.identificationData && this.identificationData.diseasesCount;
    }

    get knownDrugsCount() {
        return this.identificationData && this.identificationData.knownDrugsCount;
    }
}
