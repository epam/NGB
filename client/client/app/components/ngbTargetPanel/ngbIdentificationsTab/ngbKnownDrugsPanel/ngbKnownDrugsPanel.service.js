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

    static instance (ngbTargetPanelService, targetDataService) {
        return new ngbKnownDrugsPanelService(ngbTargetPanelService, targetDataService);
    }

    constructor(ngbTargetPanelService, targetDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService});
        this._loading = false;
        this._sourceModel = this.sourceOptions.OPEN_TARGETS;
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
}
