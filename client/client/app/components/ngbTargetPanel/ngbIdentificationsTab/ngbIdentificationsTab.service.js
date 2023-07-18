export default class NgbIdentificationsTabService {

    _openedPanels = {
        drugs: false,
        diseases: false,
        description: false,
        sequences: false,
        genomics: false,
        structure: false,
        bibliography: false
    }

    get openedPanels() {
        return this._openedPanels;
    }
    set openedPanels(value) {
        this._openedPanels = value;
    }

    static instance () {
        return new NgbIdentificationsTabService();
    }

    constructor() {
        Object.assign(this, {});
    }

    closeAll() {
        this._openedPanels = {
            drugs: false,
            diseases: false,
            description: false,
            sequences: false,
            genomics: false,
            structure: false,
            bibliography: false
        };
    }
}
