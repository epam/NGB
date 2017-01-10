import baseFilterController from '../baseFilterController';
export default class activeVcfFilesController extends baseFilterController {
    static get UID() {
        return 'activeVcfFilesController';
    }

    /** @ngInject */
    constructor(projectDataService, dispatcher, projectContext, $scope, ngbFilterService) {
        super(dispatcher, projectContext, $scope);
        this._dataProjectService = projectDataService;
        this._ngbFilterService = ngbFilterService;

        this.INIT();
        this.setDefault();
    }

    INIT() {
        const allVcfIdList = [];
        if (this.projectContext.project) {
            this.VCFs = this.projectContext.vcfTracks;
            this.VCFs.forEach(function (vcf) {
                allVcfIdList.push(vcf.id);
            });
            this._ngbFilterService.setAllVcfIdList(allVcfIdList);
        } else {
            this.VCFs = [];
        }
    }

    setDefault() {
        super.setDefault();
        this.selectedVcfs = [];
    }

    toggleVcf(item) {
        const idx = this.selectedVcfs.indexOf(item);
        if (idx > -1) {
            this.selectedVcfs.splice(idx, 1);
        }
        else {
            this.selectedVcfs.push(item);
        }
        this.emitEvent();
    }

    existsVcf(item) {
        return this.selectedVcfs.indexOf(item) > -1;
    }

    emitEvent() {
        this.projectContext.vcfFilter.vcfFileIds = this._getVcfFileIdsFromFilter();
        super.emitEvent();
    }


    _getVcfFileIdsFromFilter() {
        const idList = [];
        this.selectedVcfs.forEach(function(vcf) {
            idList.push(vcf.id);
        });
        return idList;
    }

}
