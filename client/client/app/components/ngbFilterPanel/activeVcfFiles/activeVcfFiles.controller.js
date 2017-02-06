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

        const __init = ::this.INIT;

        this._dispatcher.on('tracks:state:change', __init);

        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('tracks:state:change', __init);
        });

        this.INIT();
        this.setDefault();
    }

    INIT() {
        const allVcfIdList = [];
        if (this.projectContext.reference) {
            const vcfTracks = this.projectContext.vcfTracks.reduce((tracks, track) => {
                if (tracks.filter(t => t.bioDataItemId === track.bioDataItemId).length === 0) {
                    return [...tracks, track];
                }
                return tracks;
            }, []);
            this.VCFs = vcfTracks;
            this.VCFs.forEach(function (vcf) {
                allVcfIdList.push(vcf.bioDataItemId);
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
        const ids = this._getVcfFileIdsFromFilter();
        this.projectContext.vcfFilter.vcfFileIds = this.projectContext.vcfTracks.filter(t => ids.indexOf(t.bioDataItemId) >= 0).map(t => t.id);
        super.emitEvent();
    }


    _getVcfFileIdsFromFilter() {
        const idList = [];
        this.selectedVcfs.forEach(function(vcf) {
            idList.push(vcf.bioDataItemId);
        });
        return idList;
    }

}
