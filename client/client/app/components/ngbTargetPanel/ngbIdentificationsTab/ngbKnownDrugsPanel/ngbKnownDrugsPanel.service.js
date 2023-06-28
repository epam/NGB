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

export default class ngbKnownDrugsPanelService {

    _sourceModel = this.sourceOptions.OPEN_TARGETS;

    get sourceOptions () {
        return SOURCE_OPTIONS;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
    }

    static instance () {
        return new ngbKnownDrugsPanelService();
    }

    constructor() {
        Object.assign(this, {});
    }
}
