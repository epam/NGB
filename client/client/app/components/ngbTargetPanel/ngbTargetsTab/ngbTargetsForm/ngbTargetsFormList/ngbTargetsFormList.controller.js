export default class ngbTargetsFormListController{

    searchText = '';
    selectedItem;
    list = [];
    noCache = true;

    static get UID() {
        return 'ngbTargetsFormListController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTabService, projectContext) {
        Object.assign(this, {$scope, $timeout, ngbTargetsTabService, projectContext});
        this.searchText = this.geneModel;

        const refresh = this.refresh.bind(this);
        dispatcher.on('gene:model:updated', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('gene:model:updated', refresh);
        });
    }

    refresh() {
        this.searchText = this.geneModel;
        this.$timeout(() => this.$scope.$apply());
    }

    get geneModel() {
        if (
            this.model &&
            this.ngbTargetsTabService &&
            this.ngbTargetsTabService.targetModel &&
            this.ngbTargetsTabService.targetModel.genes &&
            this.ngbTargetsTabService.targetModel.genes[this.index]
        ) {
            return this.ngbTargetsTabService.targetModel.genes[this.index][this.model];
        }
        return undefined;
    }

    async getList (text) {
        const result = await this.ngbTargetsTabService.searchGenes(text);
        const list = result.map(item => {
            if (item.speciesScientificName) {
                item.speciesName = item.speciesScientificName;
            }
            if (item.symbol) {
                item.geneName = item.symbol;
            }
            if (item.ensemblId) {
                item.geneId = item.ensemblId;
            }
            return item;
        });
        return list;
    }

    searchTextChange(text) {
        this.ngbTargetsTabService.setGeneModel(this.index, this.field, text);
    }

    selectedItemChange(gene) {
        this.ngbTargetsTabService.selectedGeneChanged(gene, this.index);
    }
}
