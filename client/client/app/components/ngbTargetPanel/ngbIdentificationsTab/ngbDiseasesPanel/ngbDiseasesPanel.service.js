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

class NgbDiseasesPanelService {
    static instance (dispatcher) {
        return new NgbDiseasesPanelService(dispatcher);
    }

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
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
}

export {
    DiseasesTabs,
    DiseaseTabNames,
    SourceOptions,
    SourceOptionNames
};
export default NgbDiseasesPanelService;
