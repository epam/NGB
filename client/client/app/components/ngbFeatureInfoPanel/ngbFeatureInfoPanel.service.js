export default class ngbFeatureInfoPanelService {

    _isInfoBeingEdited = false;
    _hasInfoHistory = false;
    attributes = null;
    _attributeDraft = null;

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
        if (properties) {
            this.attributes = properties.map(property => {
                return {
                    name: property[0],
                    value: property[1],
                    default: true
                };
            });
        } else {
            this.attributes = properties;
        }
    }

    get attributeDraft () {
        return this._attributeDraft;
    }

    set attributeDraft(draft) {
        this._attributeDraft = draft ? {...draft} : draft;
    }

    isAttributeDuplicated (newAttribute) {
        return this.attributes.some(
            attribute => attribute.name.toLowerCase() === newAttribute.toLowerCase());
    }

    removeAttribute(property) {
        const attributes = this.attributes;
        const index = attributes.indexOf(property);
        if (index !== -1) {
            attributes.splice(index, 1);
        }
    }

    changeAttribute(property) {
        const attributes = this.attributes;
        attributes.map(attribute => {
            if (property.default) {
                if (attribute.name === property.name) {
                    attribute.value = property.value;
                }
            } else {
                if (this.isAttributeDuplicated(property.name)) {
                    attribute = [...property];
                }
            }
            return attribute;
        });
    }

    addAttribute() {
        const draft = {...this.attributeDraft};
        const attributes = this.attributes;
        attributes.push(draft);
        this.attributeDraft = {};
    }

    onChangeNewAttrubuteName(name) {
        const attribute = {...this.attributeDraft};
        attribute.name = name;
        attribute.default = false;
        this.attributeDraft = attribute;
    }

    onChangeNewAttrubuteValue(value) {
        const attribute = {...this.attributeDraft};
        attribute.value = value;
        this.attributeDraft = attribute;
    }

    onClickRemoveNewAttribute() {
        this.attributeDraft = null;
    }

    isAttributeValid () {
        if (this.attributeDraft && this.attributeDraft.name && this.attributeDraft.value) {
            if (!this.isAttributeDuplicated(this.attributeDraft.name)) {
                return true;
            }
        }
        return false;
    }

    saveNewAttributes () {
        if (this.isAttributeValid()) {
            this.addAttribute();
        }
    }
}
