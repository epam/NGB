const TARGET_STATE = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS',
    DISEASES: 'DISEASES'
};

export default class ngbTargetPanelController {

    get targetState() {
        return TARGET_STATE;
    }

    tabSelected = this.targetState.TARGET_STATE;

    get identificationTabIsShown() {
        const {identificationTarget, identificationData} = this.ngbTargetPanelService;
        return identificationTarget && identificationData;
    }

    static get UID() {
        return 'ngbTargetPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetPanelService, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, ngbTargetPanelService, ngbTargetsTabService});
        this.tabSelected = this.targetState.TARGETS;
        dispatcher.on('target:launch:finished', this.showIdentificationTab.bind(this));
        dispatcher.on('homologs:create:target', this.createTargetFromHomologs.bind(this));
    }

    get isTableMode() {
        return this.ngbTargetsTabService &&
            this.ngbTargetsTabService.isTableMode &&
            this.tabSelected === this.targetState.TARGETS;
    }

    get isIdentificationsMode() {
        return this.tabSelected === this.targetState.IDENTIFICATIONS;
    }

    addTarget () {
        if (this.ngbTargetsTabService) {
            this.ngbTargetsTabService.setAddMode();
        }
    }

    storeReport () {

    }

    changeState(state) {
        if (this.targetState.hasOwnProperty(state)) {
            this.tabSelected = this.targetState[state];
        }
        this.$timeout(() => this.$scope.$apply());
    }

    showIdentificationTab() {
        this.currentTargetState = this.targetState.IDENTIFICATIONS;
        this.tabSelected = this.targetState.IDENTIFICATIONS;
        this.$timeout(() => this.$scope.$apply());
    }

    createTargetFromHomologs() {
        this.changeState(this.targetState.TARGETS);
    }
}
