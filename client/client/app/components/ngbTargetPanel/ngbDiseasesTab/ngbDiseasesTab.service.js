export default class ngbDiseasesTabService {

    _openedPanels = {
        drugs: false,
        targets: false,
    }

    get openedPanels() {
        return this._openedPanels;
    }
    set openedPanels(value) {
        this._openedPanels = value;
    }

    static instance () {
        return new ngbDiseasesTabService();
    }

    constructor() {
        Object.assign(this, {});
        this.diseasesData = {
            knownDrugsCount: 164,
            targetsCount: 202
        };
    }

    closeAll() {
        this._openedPanels = {
            drugs: false,
            targets: false,
        };
    }
}
