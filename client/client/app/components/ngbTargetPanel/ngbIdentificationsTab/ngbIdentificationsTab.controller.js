export default class ngbIdentificationsTabController {

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
        const params = this.ngbTargetPanelService.identificationParams;
        return params && params.targetName;
    }

    get interest() {
        const params = this.ngbTargetPanelService.identificationParams;
        return params && params.interest.join(', ');
    }

    get translational() {
        const params = this.ngbTargetPanelService.identificationParams;
        return params && params.translational.join(', ');
    }

    get description() {
        return Object.values(this.identificationData.description)[0];
    }

    get shortDescription() {
        return `${this.description.substring(0, 150)}...`;
    }
}
