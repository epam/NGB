const CUSTOM_NAME = 'custom';

export default class ngbPatentsSequencesTabController {

    get customName() {
        return CUSTOM_NAME;
    }

    static get UID() {
        return 'ngbPatentsSequencesTabController';
    }

    constructor($scope, $timeout, dispatcher, ngbPatentsSequencesTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbPatentsSequencesTabService});
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:patents:proteins:updated', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:patents:proteins:updated', refresh);
        });
    }

    get requestedModel() {
        return this.ngbPatentsSequencesTabService.requestedModel;
    }

    get headerText() {
        return this.ngbPatentsSequencesTabService.headerText[this.requestedModel.searchBy];
    }

    get headerDetails() {
        const { searchBy, proteinId, proteinName, sequence, originalSequence } = this.requestedModel;
        if (searchBy === this.searchByOptions.name) {
            return proteinName;
        } else {
            const name = originalSequence === sequence ? proteinId : this.customName;
            return `${name} (length ${sequence.length} aa)`
        }
    }

    get loadingProteins() {
        return this.ngbPatentsSequencesTabService.loadingProteins;
    }
    get proteins() {
        return this.ngbPatentsSequencesTabService.proteins;
    }
    get selectedProtein() {
        return this.ngbPatentsSequencesTabService.selectedProtein;
    }
    set selectedProtein(value) {
        this.ngbPatentsSequencesTabService.selectedProtein = value;
    }
    get searchByOptions() {
        return this.ngbPatentsSequencesTabService.searchByOptions;
    }
    get searchByNames() {
        return this.ngbPatentsSequencesTabService.searchByNames;
    }
    get searchBy () {
        return this.ngbPatentsSequencesTabService.searchBy;
    }
    set searchBy (value) {
        this.ngbPatentsSequencesTabService.searchBy = value;
    }
    get searchSequence() {
        return this.ngbPatentsSequencesTabService.searchSequence;
    }
    set searchSequence(value) {
        this.ngbPatentsSequencesTabService.searchSequence = value;
    }
    get originalSequence() {
        return this.ngbPatentsSequencesTabService.originalSequence;
    }
    set originalSequence(value) {
        this.ngbPatentsSequencesTabService.originalSequence = value;
    }
    get searchDisabled() {
        return this.ngbPatentsSequencesTabService.searchDisabled;
    }
    get loadingData() {
        return this.ngbPatentsSequencesTabService.loadingData;
    }
    set loadingData(value) {
        this.ngbPatentsSequencesTabService.loadingData = value;
    }
    get failedResult() {
        return this.ngbPatentsSequencesTabService.failedResult;
    }
    get errorMessageList() {
        return this.ngbPatentsSequencesTabService.errorMessageList;
    }
    get tableResults() {
        return this.ngbPatentsSequencesTabService.tableResults;
    }
    get loadingSequence() {
        return this.ngbPatentsSequencesTabService.loadingSequence;
    }
    set loadingSequence(value) {
        this.ngbPatentsSequencesTabService.loadingSequence = value;
    }
    get failedSequence() {
        return this.ngbPatentsSequencesTabService.failedSequence;
    }
    set failedSequence(value) {
        this.ngbPatentsSequencesTabService.failedSequence = value;
    }
    get errorSequence() {
        return this.ngbPatentsSequencesTabService.errorSequence;
    }
    set errorSequence(value) {
        this.ngbPatentsSequencesTabService.errorSequence = value;
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
        this.setSequence();
    }

    onChangeProtein() {
        this.setSequence();
    }

    async setSequence() {
        this.loadingSequence = true;
        this.failedSequence = false;
        this.errorSequence = null;
        const result = await this.ngbPatentsSequencesTabService.getSequence();
        if (result) {
            const parts = result.split(']');
            const sequencePart = (parts.length > 1) ? parts[1] : parts[0];
            const sequence = sequencePart.replace(/\n/g, '');
            this.originalSequence = sequence;
            this.searchSequence = sequence;
        } else {
            this.searchSequence = '';
        }
        if (this.searchBy === this.searchByOptions.sequence) {
            this.$timeout(() => this.$scope.$apply());
        }
    }

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    onChangeSequence() {
        this.selectedProtein = undefined;
    }

    onClickSearch() {
        this.ngbPatentsSequencesTabService.resetTableResults();
        this.loadingData = true;
        if (this.searchBy === this.searchByOptions.name) {
            this.searchByProteinName();
        }
        if (this.searchBy === this.searchByOptions.sequence) {
            this.searchByProteinSequence();
        }
    }

    async searchByProteinName() {
        await this.ngbPatentsSequencesTabService.searchPatentsByProteinName();
        this.$timeout(() => {
            this.dispatcher.emit('target:identification:patents:protein:search:changed');
            this.$scope.$apply();
        });
    }

    searchByProteinSequence() {
        this.ngbPatentsSequencesTabService.searchPatentsByProteinSequence();
    }
}
