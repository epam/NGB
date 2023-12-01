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

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    onClickSearch() {
        console.log(this.selectedDrug, this.searchBy)
    }
}
