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
        if (this.projectContext.reference) {
            const ids = this._ngbFilterService.getAllVcfIdList();
            const vcfIds = this.projectContext.vcfTracks.filter(t => ids.indexOf(t.bioDataItemId) >= 0).map(t => t.id);
            return this._dataProjectService.autocompleteGeneId(
                searchText,
                this.projectContext.vcfFilter.vcfFileIds.length !== 0 ?
                    this.projectContext.vcfFilter.vcfFileIds :
                    vcfIds)
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
