const TAB_STATE = {
    DISEASES: 'DISEASES',
    DRUGS: 'DRUGS'
};

export default class ngbKnownDrugsPanelController {

    get tabState() {
        return TAB_STATE;
    }

    tabSelected = this.tabState.DISEASES;

    static get UID() {
        return 'ngbKnownDrugsPanelController';
    }

    constructor($scope, $timeout, ngbKnownDrugsPanelService) {
        Object.assign(this, {$scope, $timeout, ngbKnownDrugsPanelService});
    }

    get sourceOptions () {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    get sourceModel () {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }
    set sourceModel (value) {
        this.ngbKnownDrugsPanelService.sourceModel = value;
    }

    changeState(state) {
        if (this.tabState.hasOwnProperty(state)) {
            this.tabSelected = this.tabState[state];
        }
        this.$timeout(::this.$scope.$apply);
    }

    onChangedModel() {
        this.ngbKnownDrugsPanelService.onChangeSource(this.sourceModel);
    }
}
