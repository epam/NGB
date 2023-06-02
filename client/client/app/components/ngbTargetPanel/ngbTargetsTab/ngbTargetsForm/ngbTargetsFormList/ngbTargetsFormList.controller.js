export default class ngbTargetsFormListController{

    searchText = '';
    selectedItem;
    list = [];
    noCache = true;

    static get UID() {
        return 'ngbTargetsFormListController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, ngbTargetsTabService});
        this.searchText = this.geneModel;

        const refresh = this.refresh.bind(this);
        dispatcher.on('gene:model:updated', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('gene:model:updated', refresh);
        });
    }

    refresh() {
        this.searchText = this.geneModel;
        this.$timeout(::this.$scope.$apply);
    }

    get geneModel() {
        return this.ngbTargetsTabService.targetModel.genes[this.index][this.model];
    }

    async getList (text) {
        const result = await this.ngbTargetsTabService.searchGenes(text);
        return result;
    }

    searchTextChange(text) {
        this.ngbTargetsTabService.setGeneModel(this.index, this.field, text);
    }

    selectedItemChange(gene) {
        this.ngbTargetsTabService.selectedGeneChanged(gene, this.index);
    }
}
