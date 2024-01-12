export default class ngbTargetsFormListController{

    searchText = '';
    selectedItem;
    list = [];

    static get UID() {
        return 'ngbTargetsFormListController';
    }

    constructor($scope, $timeout, ngbTargetsTabService, projectContext) {
        Object.assign(this, {$scope, $timeout, ngbTargetsTabService, projectContext});
        this.searchText = this.value;
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
        this.changeText(this.row, this.field, text);
    }

    selectedItemChange(gene) {
        this.selectGene(this.row, gene);
    }
}
