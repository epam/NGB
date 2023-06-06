const TARGET_STATE = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS'
};

export default class ngbTargetPanelController{

    get targetState() {
        return TARGET_STATE;
    }

    get identificationTabIsShown() {
        const {identificationParams, identificationData} = this.ngbTargetPanelService;
        return identificationParams && identificationData;
    }

    static get UID() {
        return 'ngbTargetPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetPanelService) {
        Object.assign(this, {$scope, $timeout, ngbTargetPanelService});
        this.currentTargetState = this.targetState.TARGETS;
        this.tabSelected = this.targetState.TARGETS;
        dispatcher.on('target:launch:finished', this.showIdentificationTab.bind(this));
    }

    changeState(state, isRepeat) {
        if (this.targetState.hasOwnProperty(state)) {
            this.currentTargetState = this.targetState[state];
            this.tabSelected = state === this.targetState.TARGETS
                ? this.targetState.TARGETS
                : this.targetState.IDENTIFICATIONS;
        }
        this.ngbTargetPanelService.isRepeat = !!isRepeat;
        this.$timeout(::this.$scope.$apply);
    }

    showIdentificationTab() {
        this.currentTargetState = this.targetState.IDENTIFICATIONS;
        this.tabSelected = this.targetState.IDENTIFICATIONS;
        this.$timeout(::this.$scope.$apply);
    }
}
