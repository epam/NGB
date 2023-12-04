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
    get searchStructure() {
        return this.ngbPatentsChemicalsTabService.searchStructure;
    }
    set searchStructure(value) {
        this.ngbPatentsChemicalsTabService.searchStructure = value;
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

    get searchDisabled() {
        return this.ngbPatentsChemicalsTabService.searchDisabled;
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeProtein() {
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
