export default class ngbTargetsFormListController{

    searchText = '';
    selectedItem;
    list = [];
    noCache = true;

    static get UID() {
        return 'ngbTargetsFormListController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbTargetsTabService});
        this.searchText = this.geneModel;
        this.dispatcher.on('gene:model:updated', this.refresh.bind(this));
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
