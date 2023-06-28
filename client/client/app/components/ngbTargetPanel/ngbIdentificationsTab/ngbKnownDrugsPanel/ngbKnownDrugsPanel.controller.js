const TAB_STATE = {
    DISEASES: 'DISEASES',
    DRUGS: 'DRUGS'
};

const DGI_DB = 'DGI_DB';

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

    $onDestroy() {
        this.sourceModel = this.sourceOptions.OPEN_TARGETS;
        this.onChangeSource();
    }

    get sourceOptions () {
        if (this.tabSelected === this.tabState.DISEASES) {
            const options = {};
            const sources = this.ngbKnownDrugsPanelService.sourceOptions;
            for (const option in sources) {
                if (sources[option].name !== DGI_DB) {
                    options[option] = sources[option];
                }
            }
            return options;
        }
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
            if (this.tabSelected === this.tabState.DISEASES
                && this.sourceModel.name === DGI_DB) {
                this.sourceModel = this.sourceOptions.OPEN_TARGETS;
            }
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
