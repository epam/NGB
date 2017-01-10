import baseFilterController from '../baseFilterController';


export default class geneController extends baseFilterController {
    static get UID() {
        return 'geneController';
    }

    /** @ngInject */
    constructor(dispatcher, projectContext, projectDataService, $scope, ngbFilterService) {
        super(dispatcher, projectContext, $scope);

        this._dataProjectService = projectDataService;
        this._ngbFilterService = ngbFilterService;
    }

    getGenes(searchText) {
        if (this.projectId) {
            return this._dataProjectService.autocompleteGeneId(
                this.projectId,
                searchText,
                this.projectContext.vcfFilter.vcfFileIds.length !== 0 ?
                    this.projectContext.vcfFilter.vcfFileIds :
                    this._ngbFilterService.getAllVcfIdList())
                .then((data)=> data
                );
        } else {
            return [];
        }
    }

    addGeneChip(item) {
        const idx = this.projectContext.vcfFilter.selectedGenes.indexOf(item);
        if (idx === -1) {
            this.projectContext.vcfFilter.selectedGenes.push(item);
        }
        this.emitEvent();
    }

    removeGeneChip(item) {
        const idx = this.projectContext.vcfFilter.selectedGenes.indexOf(item);
        if (idx !== -1) {
            this.projectContext.vcfFilter.selectedGenes.splice(idx, 1);
        }
        this.emitEvent();
    }

}
