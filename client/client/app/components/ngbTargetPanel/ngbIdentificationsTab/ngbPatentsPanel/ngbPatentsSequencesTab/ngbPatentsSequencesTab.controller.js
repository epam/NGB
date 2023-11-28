export default class ngbPatentsSequencesTabController {

    static get UID() {
        return 'ngbPatentsSequencesTabController';
    }

    constructor($scope, $timeout, dispatcher, ngbPatentsSequencesTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbPatentsSequencesTabService});
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:patents:sequences:proteins:updated', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:patents:sequences:proteins:updated', refresh);
        });
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

    $onInit() {
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    onClickSearch() {
        console.log(this.selectedProtein, this.searchBy)
    }
}
