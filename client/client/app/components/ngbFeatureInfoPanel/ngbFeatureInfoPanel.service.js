export default class ngbFeatureInfoPanelService {

    _isInfoBeingEdited = false;
    _hasInfoHistory = false;

    static instance() {
        return new ngbFeatureInfoPanelService();
    }

    constructor() {}

    get isInfoBeingEdited () {
        return this._isInfoBeingEdited;
    }

    set isInfoBeingEdited (value) {
        this._isInfoBeingEdited = value;
    }

    get hasInfoHistory () {
        return this._hasInfoHistory;
    }

    set hasInfoHistory (value) {
        this._hasInfoHistory = value;
    }
}
