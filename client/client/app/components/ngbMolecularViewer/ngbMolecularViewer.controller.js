import  baseController from '../../shared/baseController';
import {EventGeneInfo} from '../../shared/utils/events';
import ngbMolecularViewerService from './ngbMolecularViewer.service';

export default class ngbMolecularViewerController extends baseController {
    static get UID() {
        return 'ngbMolecularViewerController';
    }

    PDBids = null;
    geneTracks = [];
    selectedGeneTrack = null;
    service: ngbMolecularViewerService;
    event: EventGeneInfo = null;
    selectedItemId = null;
    selectedItem = null;
    region = null;

    constructor($sce, $scope, dispatcher, ngbMolecularViewerService, ngbMolecularViewerConstant) {
        super();

        Object.assign(this, {$scope, dispatcher, ngbMolecularViewerConstant, ngbMolecularViewerService});

        this.defaultMessage = $sce.trustAsHtml(ngbMolecularViewerConstant.defaultMessage);


        this.initEvents();

        this.$scope.$watch('$ctrl.selectedItemId', () => {
            this.descriptionDone = false;
            if (!this.PDBids || this.PDBids.length === 0) {
                this.selectedItem = null;
            }
            else {
                this.selectedItem = this.PDBids.filter(x => x.id === this.selectedItemId)[0];
            }
        });

        this.$scope.$watch('$ctrl.selectedGeneTrack', ::this.geneTrackChanged);

        this.descriptionDone = false;
    }

    events = {
        'miew:show:structure': ::this.geneClick,
        'miew:highlight:region': ::this.geneClick
    };

    async geneTrackChanged() {
        if (!this.selectedGeneTrack || !this.event)
            return;
        this.descriptionDone = false;
        this.PDBids = null;
        this.errorMessageList = [];
        this.selectedItem = null;
        this.selectedItemId = null;

        this.geneName = event.geneId;
        this.loading = true;

        const [track] = this.geneTracks.filter(x => x.id === this.selectedGeneTrack);

        try {
            const listPDBids = await this.ngbMolecularViewerService.loadPDB(track, this.event);
            if (listPDBids.length > 0) {
                const distinctList = Array.from(new Set(listPDBids.map(m => m.id)));

                this.PDBids = distinctList.map(m => listPDBids.filter(p => p.id === m)[0]);
                this.selectedItemId = this.PDBids[0].id;

                this.mdSelectSource = this.PDBids.map(m => listPDBids.filter(p => p.id === m.id)[0]);

            } else {
                this.errorMessageList.push(this.ngbMolecularViewerConstant.errorNoPDB);
            }

        }
        catch (exception) {
            this.errorMessageList.push((exception && exception.message)
                ? exception.message
                : this.ngbMolecularViewerConstant.unhandledEx);
        }
        this.loading = false;
        this.$scope.$apply();
    }

    async geneClick(event: EventGeneInfo) {
        if (event.highlight) {
            this.region = {
                startIndex: event.startIndex,
                endIndex: event.endIndex
            };
        }
        else {
            this.region = null;
        }
        const shouldReload = this.selectedGeneTrack && this.selectedGeneTrack === event.geneTracks[0].id;
        this.event = event;
        this.geneTracks = event.geneTracks;
        this.selectedGeneTrack = event.geneTracks[0].id;
        if (shouldReload) {
            await this.geneTrackChanged();
        }
    }


}
