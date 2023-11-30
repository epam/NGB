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
    get searchSequence() {
        return this.ngbPatentsSequencesTabService.searchSequence;
    }
    set searchSequence(value) {
        this.ngbPatentsSequencesTabService.searchSequence = value;
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
    set failedResult(value) {
        this.ngbPatentsSequencesTabService.failedResult = value;
    }
    get errorMessageList() {
        return this.ngbPatentsSequencesTabService.errorMessageList;
    }
    set errorMessageList(value) {
        this.ngbPatentsSequencesTabService.errorMessageList = value;
    }

    $onInit() {
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeProtein() {
        this.selectedProtein;
    }

    onChangeSearchBy(option) {
        this.searchBy = option;
    }

    async onClickSearch() {
        this.loadingData = true;
        this.failedResult = false;
        this.errorMessageList = null;
        const result = await this.ngbPatentsSequencesTabService.searchPatents();
        if (result) {
            this.$timeout(() => this.$scope.$apply());
        }
    }
}
