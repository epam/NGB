const SEARCH_BY_OPTIONS = {
    name: 'name',
    sequence: 'sequence',
};

const SEARCH_BY_NAMES = {
    [SEARCH_BY_OPTIONS.name]: 'protein name',
    [SEARCH_BY_OPTIONS.sequence]: 'amino acid sequence',
}

export default class ngbPatentsSequencesTabController {

    get searchByOptions() {
        return SEARCH_BY_OPTIONS;
    }

    get searchByNames() {
        return SEARCH_BY_NAMES;
    }

    searchBy = this.searchByOptions.name;

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
