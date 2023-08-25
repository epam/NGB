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
};

const SourceOptionNames = {
    [SourceOptions.OPEN_TARGETS]: 'Open Targets',
    [SourceOptions.PHARM_GKB]: 'PharmGKB'
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

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    exportResults() {
        const source = this.exportSource[this.sourceModel];
        if (!this.geneIds) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        return this.targetDataService.getTargetExport(this.geneIds, source);
    }
}

export {
    DiseasesTabs,
    DiseaseTabNames,
    SourceOptions,
    SourceOptionNames
};
export default NgbDiseasesPanelService;
