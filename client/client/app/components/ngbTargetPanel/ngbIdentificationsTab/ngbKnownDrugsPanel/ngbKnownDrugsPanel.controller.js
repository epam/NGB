const TAB_STATE = {
    DISEASES: 'DISEASES',
    DRUGS: 'DRUGS'
};

export default class ngbKnownDrugsPanelController {

    currentTabState;
    tabSelected;

    sourceList = ['Open Targets'];
    sourceModel = null;

    get tabState() {
        return TAB_STATE;
    }

    static get UID() {
        return 'ngbKnownDrugsPanelController';
    }

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
        this.currentTabState = this.tabState.DISEASES;
        this.tabSelected = this.tabState.DISEASES;
    }

    changeState(state, isRepeat) {
        if (this.tabState.hasOwnProperty(state)) {
            this.currentTabState = this.tabState[state];
            this.tabSelected = state === this.tabState.DISEASES
                ? this.tabState.DISEASES
                : this.tabState.DRUGS;
        }
        this.$timeout(::this.$scope.$apply);
    }

    onChangedModel() {
    }
}
