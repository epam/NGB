export default class ngbFeatureInfoPanelService {

    _editMode = false;
    _hasInfoHistory = false;
    attributes = null;

    static instance() {
        return new ngbFeatureInfoPanelService();
    }

    constructor() {}

    get editMode () {
        return this._editMode;
    }

    set editMode (value) {
        this._editMode = value;
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

    isAttributeDuplicated (newAttribute) {
        return !this.attributes.some(
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

    saveNewAttributes () {
        this.attributes = this.attributes.filter(attribute => attribute.name && attribute.value);
    }

    someAttributeIsEmpty () {
        const attributes = this.newAttributes;
        return attributes.some(attribute => {
            if (!attribute.name && !attribute.value) {
                return attribute.default;
            }
            if (!attribute.name || !attribute.value) {
                return true;
            }
        });
    }
}
