export default class ngbRegisterTrackController {
    static get UID() {
        return 'ngbRegisterTrackController';
    }

    _registerModes = null;
    _registerModesNames = null;
    _registerMode = null;

    /** @ngInject */
    constructor(ngbRegisterTrackConstants) {
        this._registerModes = ngbRegisterTrackConstants.registerModes;
        this._registerModesNames = ngbRegisterTrackConstants.names;
        for (let i = 0; i < this._registerModes.length; i++) {
            if (this._registerModes[i].isDefault) {
                this._registerMode = this._registerModes[i].value;
                break;
            }
        }
    }
}