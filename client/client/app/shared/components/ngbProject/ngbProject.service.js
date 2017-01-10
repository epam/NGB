function referenceAndTracks() {
    return {
        referencesBioDataItemIdList: [],
        tracksBioDataItemIdList: []
    };
}

export default class ngbProjectService {

    constructor(dispatcher, genomeDataService, vcfDataService, bamDataService, geneDataService, wigDataService, bedDataService, segDataService, mafDataService) {
        this._dispatcher = dispatcher;
        this._trackServices = {
            'bam': bamDataService,
            'vcf': vcfDataService,
            'gene': geneDataService,
            'wig': wigDataService,
            'bed': bedDataService,
            'seg': segDataService,
            'maf': mafDataService
        };
        this._genomeDataService = genomeDataService;
        this.INIT();
    }

    INIT() {
        this.setDefault();
        this._genomeDataService.loadAllReference().then((references) => {
            this.references = references;
        });
    }

    referenceChange(referenceChangeEvent) {
        this.referenceList = referenceChangeEvent.referenceList || [];
        this.projectTracks = new referenceAndTracks();
        this.allTracks = {};
        const tracksFormat = Object.keys(this._trackServices);
        for (const format of tracksFormat) {
            this.allTracks[format] = [];
        }
        this.referenceList.forEach((reference) => {
            const referenceForSave = {
                bioDataItemId: reference.bioDataItemId,
                hidden: false
            };
            this.projectTracks.referencesBioDataItemIdList.push(referenceForSave);
            this.loadTracksFromServer(reference, this.allTracks);
        });
    }

    onNewTrackRegister() {
        this.allTracks = {};
        const tracksFormat = Object.keys(this._trackServices);
        for (const format of tracksFormat) {
            this.allTracks[format] = [];
        }
        this.referenceList.forEach((reference) => {
            this.loadTracksFromServer(reference, this.allTracks);
        });
    }

    async loadTracksFromServer(reference, allTracks) {
        for (const track in allTracks) {
            if (allTracks.hasOwnProperty(track)) {
                const tracksFromServer = await this._trackServices[track].getAllFileList(reference.id);
                allTracks[track].push(...tracksFromServer);
            }
        }
    }

    tracksChange(tracksChangeEvent) {
        this.projectTracks.tracksBioDataItemIdList = tracksChangeEvent.selectedTrackList;
    }

    setDefault() {
        this.projectTracks = new referenceAndTracks();
        this.referenceList = [];
        this.allTracks = {};
        const tracksFormat = Object.keys(this._trackServices);
        for (const format of tracksFormat) {
            this.allTracks[format] = [];
        }
        this.projectName = '';
    }

    setProjectName(name) {
        this.projectName = name;
    }

    setProjectId(id) {
        this.projectId = id;
    }

    getReferences() {
        return this.references;
    }

    getSelectedReferences() {
        return this.referenceList;
    }

    getAllTracks() {
        return this.allTracks;
    }

    getSavingProjectObject() {
        return {
            projectTracks: this.projectTracks,
            projectName: this.projectName,
            id: this.projectId
        };
    }

    isReferenceSelected() {
        return this.referenceList
            ? this.referenceList.length > 0
            : false;
    }
}