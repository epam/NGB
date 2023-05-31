export default class ngbTargetPanelController{

    targetState;

    static get UID() {
        return 'ngbTargetPanelController';
    }

    constructor($scope, $timeout, ngbTargetPanelService) {
        Object.assign(this, {$scope, $timeout, ngbTargetPanelService});
        this.targetState = ngbTargetPanelService.targetState;
        this.currentTargetState = this.targetState.TARGETS;
        this.tabSelected = this.targetState.TARGETS;
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
}
