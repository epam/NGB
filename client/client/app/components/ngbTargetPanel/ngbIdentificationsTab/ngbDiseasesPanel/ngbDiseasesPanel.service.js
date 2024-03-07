const DiseasesTabs = {
    table: 'table',
    bubbles: 'bubbles',
    graph: 'graph'
};

const DiseaseTabNames = {
    [DiseasesTabs.table]: 'Table',
    [DiseasesTabs.bubbles]: 'Bubbles',
    [DiseasesTabs.graph]: 'Graph'
};

const SourceOptions = {
    OPEN_TARGETS: 'OPEN_TARGETS',
    PHARM_GKB: 'PHARM_GKB',
    TTD: 'TTD'
};

const SourceOptionNames = {
    [SourceOptions.OPEN_TARGETS]: 'Open Targets',
    [SourceOptions.PHARM_GKB]: 'PharmGKB',
    [SourceOptions.TTD]: 'Therapeutic Target Database'
};

const EXPORT_SOURCE = {
    OPEN_TARGETS: 'OPEN_TARGETS_DISEASES',
    PHARM_GKB: 'PHARM_GKB_DISEASES'
};

class NgbDiseasesPanelService {

    get exportSource () {
        return EXPORT_SOURCE;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService) {
        return new NgbDiseasesPanelService(dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService});
        this._sourceModel = SourceOptions.OPEN_TARGETS;
        this._tableLoading = false;
        this._chartsLoading = false;
        this._exportLoading = false;
    }

    get sourceModel() {
        return this._sourceModel;
    }

    set sourceModel(sourceModel) {
        if (this._sourceModel !== sourceModel) {
            this._sourceModel = sourceModel;
            this.dispatcher.emit('target:identification:diseases:source:changed', this);
        }
    }

    get tableLoading() {
        return this._tableLoading;
    }

    set tableLoading(tableLoading) {
        this._tableLoading = tableLoading;
    }

    get chartsLoading() {
        return this._chartsLoading;
    }

    set chartsLoading(chartsLoading) {
        this._chartsLoading = chartsLoading;
    }
    get exportLoading() {
        return this._exportLoading;
    }

    set exportLoading(value) {
        this._exportLoading = value;
    }

    get geneIdsOfInterest() {
        return this.ngbTargetPanelService.geneIdsOfInterest;
    }

    get translationalGeneIds() {
        return this.ngbTargetPanelService.translationalGeneIds;
    }

    exportResults() {
        const source = this.exportSource[this.sourceModel];
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

export {
    DiseasesTabs,
    DiseaseTabNames,
    SourceOptions,
    SourceOptionNames
};
export default NgbDiseasesPanelService;
