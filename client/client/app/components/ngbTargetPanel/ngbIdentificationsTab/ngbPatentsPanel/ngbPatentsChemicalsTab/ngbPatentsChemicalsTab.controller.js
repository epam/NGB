export default class ngbPatentsChemicalsTabController {

    static get UID() {
        return 'ngbPatentsChemicalsTabController';
    }

    constructor($scope, ngbPatentsChemicalsTabService) {
        Object.assign(this, {$scope, ngbPatentsChemicalsTabService});
    }

    get loadingDrugs() {
        return this.ngbPatentsChemicalsTabService.loadingDrugs;
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

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    onClickSearch() {
        console.log(this.selectedDrug, this.searchBy)
    }
}
