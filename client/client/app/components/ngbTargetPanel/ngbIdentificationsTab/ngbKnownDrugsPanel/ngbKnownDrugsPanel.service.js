const SOURCE_OPTIONS = {
    OPEN_TARGETS: {
        displayName: 'Open Targets',
        name: 'OPEN_TARGETS'
    },
    DGI_DB: {
        displayName: 'DGIdb',
        name: 'DGI_DB'
    },
    PHARM_GKB: {
        displayName: 'PharmGKB',
        name: 'PHARM_GKB'
    }
};

const EXPORT_SOURCE = {
    OPEN_TARGETS: 'OPEN_TARGETS_DRUGS',
    PHARM_GKB: 'PHARM_GKB_DRUGS',
    DGI_DB: 'DGIDB_DRUGS'
};

export default class ngbKnownDrugsPanelService {

    get exportSource () {
        return EXPORT_SOURCE;
    }

    _tmapLoading = false;
    _tmapFailed = false;
    _tmapErrorList = null;
    _tmapUrl;

    get tmapLoading() {
        return this._tmapLoading;
    }
    set tmapLoading(value) {
        this._tmapLoading = value;
    }
    get tmapFailed() {
        return this._tmapFailed;
    }
    get tmapErrorList() {
        return this._tmapErrorList;
    }
    get tmapUrl() {
        return this._tmapUrl;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbKnownDrugsPanelService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
        this._loading = false;
        this._sourceModel = this.sourceOptions.OPEN_TARGETS;
        dispatcher.on('target:identification:reset', this.resetData.bind(this));
    }

    get sourceOptions () {
        return SOURCE_OPTIONS;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
    }

    get loading() {
        return this._loading;
    }

    set loading(loading) {
        this._loading = !!loading;
    }

    get geneIdsOfInterest() {
        return this.ngbTargetPanelService.geneIdsOfInterest;
    }

    get translationalGeneIds() {
        return this.ngbTargetPanelService.translationalGeneIds;
    }

    exportResults() {
        const source = this.exportSource[this.sourceModel.name];
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        if (this.translationalGeneIds && !this.translationalGeneIds.length) {
            return this.targetDataService.getTargetExportGeneId(this.geneIdsOfInterest[0], source);
        }
        return this.targetDataService.getTargetExport(this.geneIdsOfInterest, this.translationalGeneIds, source);
    }

    async generateTMAP() {
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => {
                this._tmapLoading = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.generateTMAP(this.geneIdsOfInterest)
                .then(data => {
                    this._tmapFailed = false;
                    this._tmapErrorList = null;
                    this._tmapLoading = false;
                    this._tmapUrl = data;
                    resolve(true);
                })
                .catch(err => {
                    this._tmapFailed = true;
                    this._tmapErrorList = [err.message];
                    this._tmapLoading = false;
                    this._tmapUrl = undefined;
                    resolve(false);
                });
        });
    }

    resetData() {
        this._tmapLoading = false;
        this._tmapFailed = false;
        this._tmapErrorList = null;
        this._tmapUrl = undefined;
    }
}
