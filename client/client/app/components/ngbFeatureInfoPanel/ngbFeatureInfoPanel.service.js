export default class ngbFeatureInfoPanelService {

    _isGeneInfoEdited = false;
    _isGeneInfoHistory = false;

    static instance() {
        return new ngbFeatureInfoPanelService();
    }

    constructor() {}

    get isGeneInfoEdited () {
        return this._isGeneInfoEdited;
    }

    set isGeneInfoEdited (value) {
        this._isGeneInfoEdited = value;
    }

    get isGeneInfoHistory () {
        return this._isGeneInfoHistory;
    }

    set isGeneInfoHistory (value) {
        this._isGeneInfoHistory = value;
    }
}
