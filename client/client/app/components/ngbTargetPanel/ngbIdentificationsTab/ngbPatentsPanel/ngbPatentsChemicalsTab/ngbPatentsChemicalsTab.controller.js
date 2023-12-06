export default class ngbPatentsChemicalsTabController {

    static get UID() {
        return 'ngbPatentsChemicalsTabController';
    }

    constructor($scope, $timeout, dispatcher, ngbPatentsChemicalsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbPatentsChemicalsTabService});
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:patents:sequences:drugs:updated', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:patents:sequences:drugs:updated', refresh);
        });
    }

    get loadingDrugs() {
        return this.ngbPatentsChemicalsTabService.loadingDrugs;
    }
    get failedDrugs() {
        return this.ngbPatentsChemicalsTabService.failedDrugs;
    }
    get errorDrugsMessage() {
        return this.ngbPatentsChemicalsTabService.errorDrugsMessage;
    }
    get drugs() {
        return this.ngbPatentsChemicalsTabService.drugs;
    }
    get selectedDrug() {
        return this.ngbPatentsChemicalsTabService.selectedDrug;
    }
    set selectedDrug(value) {
        this.ngbPatentsChemicalsTabService.selectedDrug = value;
    }
    get searchByOptions() {
        return this.ngbPatentsChemicalsTabService.searchByOptions;
    }
    get searchByNames() {
        return this.ngbPatentsChemicalsTabService.searchByNames;
    }
    get searchBy () {
        return this.ngbPatentsChemicalsTabService.searchBy;
    }
    set searchBy (value) {
        this.ngbPatentsChemicalsTabService.searchBy = value;
    }
    get loadingData() {
        return this.ngbPatentsChemicalsTabService.loadingData;
    }
    set loadingData(value) {
        this.ngbPatentsChemicalsTabService.loadingData = value;
    }
    get failedResult() {
        return this.ngbPatentsChemicalsTabService.failedResult;
    }
    set failedResult(value) {
        this.ngbPatentsChemicalsTabService.failedResult = value;
    }
    get errorMessageList() {
        return this.ngbPatentsChemicalsTabService.errorMessageList;
    }
    set errorMessageList(value) {
        this.ngbPatentsChemicalsTabService.errorMessageList = value;
    }
    get tableResults() {
        return this.ngbPatentsChemicalsTabService.tableResults;
    }

    get searchDisabled() {
        return this.ngbPatentsChemicalsTabService.searchDisabled;
    }

    get loadingIdentifier() {
        return this.ngbPatentsChemicalsTabService.loadingIdentifier;
    }
    set loadingIdentifier(value) {
        this.ngbPatentsChemicalsTabService.loadingIdentifier = value;
    }
    get failedIdentifier() {
        return this.ngbPatentsChemicalsTabService.failedIdentifier;
    }
    set failedIdentifier(value) {
        this.ngbPatentsChemicalsTabService.failedIdentifier = value;
    }
    get errorIdentifier() {
        return this.ngbPatentsChemicalsTabService.errorIdentifier;
    }
    set errorIdentifier(value) {
        this.ngbPatentsChemicalsTabService.errorIdentifier = value;
    }

    get searchStructure() {
        return this.ngbPatentsChemicalsTabService.searchStructure;
    }
    set searchStructure(value) {
        this.ngbPatentsChemicalsTabService.searchStructure = value;
    }
    get originalStructure() {
        return this.ngbPatentsChemicalsTabService.originalStructure;
    }
    set originalStructure(value) {
        this.ngbPatentsChemicalsTabService.originalStructure = value;
    }

    refresh() {
        this.setIdentifier();
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeDrug() {
        this.setIdentifier();
    }

    async setIdentifier() {
        this.loadingIdentifier = true;
        this.failedIdentifier = false;
        this.errorIdentifier = null;
        const result = await this.ngbPatentsChemicalsTabService.getIdentifier();
        if (result) {
            this.originalStructure = result;
            this.searchStructure = result;
        } else {
            this.searchStructure = '';
        }
        if (this.ngbPatentsChemicalsTabService.isSearchByDrugStructure) {
            this.$timeout(() => this.$scope.$apply());
        }
    }

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    async onClickSearch() {
        this.loadingData = true;
        this.failedResult = false;
        this.errorMessageList = null;
        await this.ngbPatentsChemicalsTabService.searchPatents();
        this.$timeout(() => {
            this.dispatcher.emit('target:identification:patents:drug:changed');
            this.$scope.$apply();
        });
    }
}
