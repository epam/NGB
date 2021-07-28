export default class ngbFeatureInfoPanelService {

    _isInfoBeingEdited = false;
    _hasInfoHistory = false;
    attributes = null;

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

    get newAttributes () {
        return this.attributes;
    }

    set newAttributes (properties) {
        this.attributes = [...properties.map(property => [...property])];
    }

    removeAttribute(property) {
        const attributes = this.newAttributes;
        const index = attributes.indexOf(property);
        attributes.splice(index, 1);
    }

    changeAttribute(property) {
        const attributes = this.newAttributes;
        attributes.map(attribute => {
            if (attribute[0] === property[0]) {
                attribute[1] = property[1];
            }
            return attribute;
        });
    }
}
