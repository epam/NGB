const SOURCE_OPTIONS = {
    OPEN_TARGET: 'Open Targets',
    TXGNN: 'TxGNN',
    DGIDB: 'DGIdb',
    PHARMGKB: 'PharmGKB'
};

export default class ngbKnownDrugsPanelService {

    _sourceModel = this.sourceOptions.OPEN_TARGET;

    get sourceOptions () {
        return SOURCE_OPTIONS;
    }

    get sourceModel () {
        return this._sourceModel;
    }
    set sourceModel (value) {
        this._sourceModel = value;
    }

    static instance () {
        return new ngbKnownDrugsPanelService();
    }

    constructor() {
        Object.assign(this, {});
    }

    onChangeSource(source) {
        console.log('onChangeSource', source);
    }
}
