const TAB_STATE = {
    DISEASES: 'DISEASES',
    DRUGS: 'DRUGS'
};

export default class ngbKnownDrugsPanelController {

    tabSelected = this.tabState.DISEASES;

    get tabState() {
        return TAB_STATE;
    }

    static get UID() {
        return 'ngbKnownDrugsPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbKnownDrugsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbKnownDrugsPanelService});
    }

    get sourceOptions () {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbKnownDrugsPanelService.sourceModel = value;
    }

    onChangeTabState(state) {
        if (this.tabState.hasOwnProperty(state)) {
            this.tabSelected = this.tabState[state];
            this.onChangeSource();
        }
        this.$timeout(::this.$scope.$apply);
    }

    onChangeSource() {
        if (this.tabSelected === this.tabState.DRUGS) {
            this.dispatcher.emit('drugs:source:changed');
        }
        if (this.tabSelected === this.tabState.DISEASES) {
            this.dispatcher.emit('diseases:source:changed');
        }
    }
}
