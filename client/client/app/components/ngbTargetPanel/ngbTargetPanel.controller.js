const TARGET_STATE = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS'
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

    constructor($scope, $timeout, dispatcher, ngbTargetPanelService) {
        Object.assign(this, {$scope, $timeout, ngbTargetPanelService});
        this.tabSelected = this.targetState.TARGETS;
        dispatcher.on('target:launch:finished', this.showIdentificationTab.bind(this));
    }

    changeState(state) {
        if (this.targetState.hasOwnProperty(state)) {
            this.tabSelected = this.targetState[state];
        }
        this.$timeout(::this.$scope.$apply);
    }

    showIdentificationTab() {
        this.currentTargetState = this.targetState.IDENTIFICATIONS;
        this.tabSelected = this.targetState.IDENTIFICATIONS;
        this.$timeout(::this.$scope.$apply);
    }
}
