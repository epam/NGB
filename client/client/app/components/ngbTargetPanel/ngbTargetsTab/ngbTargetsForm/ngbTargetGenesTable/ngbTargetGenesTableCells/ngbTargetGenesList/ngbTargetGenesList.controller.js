export default class ngbTargetGenesListController{

    searchText = '';
    selectedItem;
    list = [];

    static get UID() {
        return 'ngbTargetGenesListController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsFormService, projectContext) {
        Object.assign(this, {$scope, $timeout, ngbTargetsFormService, projectContext});
        this.searchText = this.value;
        dispatcher.on('target:form:genes:results:updated', this.refresh.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:form:genes:results:updated', this.refresh.bind(this));
        });
    }

    refresh() {
        this.searchText = this.value;
    }

    get changeAllowed() {
        return this.ngbTargetsFormService.isAddMode || this.ngbTargetsFormService.targetModel.changeAllowed;
    }

    async getList (text) {
        const result = await this.ngbTargetsFormService.searchGenes(text);
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
