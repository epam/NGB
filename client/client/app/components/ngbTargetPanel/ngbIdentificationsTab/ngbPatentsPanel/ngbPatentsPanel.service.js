const NCBI_SOURCE = 'NCBI';
const GOOGLE_PATENTS_SOURCE = 'GOOGLE_PATENTS';

const SOURCE_OPTIONS = {
    NCBI: {
        displayName: 'NCBI',
        name: NCBI_SOURCE
    },
    GOOGLE_PATENTS: {
        displayName: 'Google Patents',
        name: GOOGLE_PATENTS_SOURCE
    }
};

export { NCBI_SOURCE, GOOGLE_PATENTS_SOURCE };

export default class ngbPatentsPanelService {
    _sourceModel = SOURCE_OPTIONS.NCBI;
    static instance (dispatcher) {
        return new ngbPatentsPanelService(dispatcher);
    }

    constructor(dispatcher) {
        Object.assign(this, {dispatcher});
        this._sourceModel = this.sourceOptions.NCBI;
    }

    get sourceOptions () {
        return SOURCE_OPTIONS;
    }

    get sourceModel() {
        return this._sourceModel;
    }
    set sourceModel(value) {
        this._sourceModel = value;
        this.dispatcher.emit('target:identification:patents:source:changed');
    }
    resetData() {
        this._sourceModel = this.sourceOptions.OPEN_TARGETS;
    }
}
