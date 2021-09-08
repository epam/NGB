import  baseController from '../../shared/baseController';
import MiewContext from '../../shared/miewContext';
import {EventGeneInfo} from '../../shared/utils/events';
import ngbMolecularViewerService from './ngbMolecularViewer.service';

export default class ngbMolecularViewerController extends baseController {
    static get UID() {
        return 'ngbMolecularViewerController';
    }

    PDBids = null;
    geneTracks = [];
    appLayout;
    selectedGeneTrack = null;
    service: ngbMolecularViewerService;
    event: EventGeneInfo = null;
    selectedItemId = null;
    selectedItem = null;
    region = null;
    currentMode = null;
    currentColor = null;
    miewContext: MiewContext = undefined;

    constructor(
        $sce,
        $scope,
        $mdMenu,
        dispatcher,
        ngbMolecularViewerService,
        ngbMolecularViewerConstant,
        miewSettings,
        miewContext,
        appLayout
    ) {
        super();

        Object.assign(
            this,
            {
                $scope,
                dispatcher,
                ngbMolecularViewerConstant,
                ngbMolecularViewerService,
                miewContext,
                appLayout
            }
        );

        this.defaultMessage = $sce.trustAsHtml(ngbMolecularViewerConstant.defaultMessage);
        this.colorer = miewSettings.displayColors;
        this.modes = miewSettings.displayModes;
        this.menu = $mdMenu;

        this.initEvents();

        this.$scope.$watch('$ctrl.selectedItemId', () => {
            this.descriptionDone = false;
            if (!this.PDBids || this.PDBids.length === 0) {
                this.selectedItem = null;
            } else {
                this.selectedItem = this.PDBids.filter(x => x.id === this.selectedItemId)[0];
            }
            this.updateMiewContext();
        });
        this.$scope.$watch('$ctrl.descriptionChainId', this.updateMiewContext.bind(this));
        this.$scope.$watch('$ctrl.selectedGeneTrack', ::this.geneTrackChanged);

        this.descriptionDone = false;
        this.miewContext.emit();
    }

    events = {
        'layout:active:panel:change': this.activePanelChanged.bind(this),
        'miew:show:structure': this.geneClick.bind(this),
        'miew:highlight:region': this.geneClick.bind(this)
    };

    activePanelChanged (o) {
        const isActive = o === this.appLayout.Panels.molecularViewer.panel;
        this.dispatcher.emit('miew:panel:active', isActive);
    }

    resetCamera () {
        this.camera = undefined;
    }

    async geneTrackChanged() {
        if (!this.selectedGeneTrack || !this.event) {
            this.hash = undefined;
            return;
        }
        const [track] = this.geneTracks.filter(x => x.id === this.selectedGeneTrack);
        const hash = this.ngbMolecularViewerService.getPDBKey(track, this.event);
        if (this.hash === hash) {
            return Promise.resolve();
        }
        this.hash = hash;

        this.descriptionDone = false;
        this.PDBids = null;
        this.errorMessageList = [];
        this.selectedItem = null;

        this.selectedItemId = null;
        this.geneName = this.event.geneId;

        this.loading = true;

        try {
            const listPDBids = await this.ngbMolecularViewerService.loadPDB(track, this.event);
            if (listPDBids.length > 0) {
                const distinctList = Array.from(new Set(listPDBids.map(m => m.id)));

                this.PDBids = distinctList.map(m => listPDBids.filter(p => p.id === m)[0]);
                this.selectedItemId = this.event.pdb || this.PDBids[0].id;
                this.mdSelectSource = this.PDBids.map(m => listPDBids.filter(p => p.id === m.id)[0]);
                this.descriptionChainId = this.event.chain;
                this.camera = this.event.camera;
                this.currentMode = this.event.mode;
                this.currentColor = this.event.color;
                this.updateMiewContext();
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
        const trackId = event.trackId || event.geneTracks[0].id;
        this.event = event;
        this.geneTracks = event.geneTracks;
        this.selectedGeneTrack = trackId;
        await this.geneTrackChanged();
    }
    loadImage(imagePath) {
        return require(`../../assets/images/${imagePath}`);
    }
    changeDisplaySettings(name, type) {
        if (type === 'mode') {
            this.currentMode = name;
        }
        if (type === 'color') {
            this.currentColor = name;
        }
        this.updateMiewContext();
    }
    updateMiewContext () {
        const pdb = this.selectedItemId;
        const mode = this.currentMode;
        const color = this.currentColor;
        const trackId = this.selectedGeneTrack;
        const [track] = (this.geneTracks || []).filter(x => x.id === this.selectedGeneTrack);
        const chain = this.descriptionChainId;
        const {
            startIndex,
            endIndex,
            geneId,
            transcriptId,
            highlight
        } = this.event || {};
        this.miewContext.update({
            startIndex,
            endIndex,
            geneId,
            transcriptId,
            highlight,
            pdb,
            mode,
            color,
            trackId,
            geneTracks: track ? [track] : undefined,
            chain
        });
    }
}
