export default class ngbIdentificationsTabService {

    _isOpen = {
        drugs: false,
        description: false,
        sequences: false,
        genomics: false,
        structure: false,
        bibliography: false
    }

    get isOpen() {
        return this._isOpen;
    }
    set isOpen(value) {
        this._isOpen = value;
    }

    static instance () {
        return new ngbIdentificationsTabService();
    }

    constructor() {
        Object.assign(this, {});
    }
}
