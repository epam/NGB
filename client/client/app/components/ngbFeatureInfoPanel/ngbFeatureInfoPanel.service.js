export default class ngbFeatureInfoPanelService {

    _editMode = false;
    _hasInfoHistory = false;
    attributes = null;
    _saveError = null;
    _saveInProgress = false;
    duplicate = false;

    static instance(geneDataService) {
        return new ngbFeatureInfoPanelService(geneDataService);
    }

    constructor(geneDataService) {
        this.geneDataService = geneDataService;
    }

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

    get saveError () {
        return this._saveError;
    }

    set saveError(error) {
        this._saveError = error;
    }

    get saveInProgress () {
        return this._saveInProgress;
    }

    set saveInProgress (progress) {
        this._saveInProgress = progress;
    }

    removeAttribute(property) {
        const attributes = this.attributes;
        const index = attributes.indexOf(property);
        if (index !== -1) {
            if (property.default) {
                attributes[index].deleted = true;
            } else {
                attributes.splice(index, 1);
            }
        }
    }

    changeAttribute(property) {
        const attributes = this.attributes;
        if (!property.default) {
            return attributes.some(attribute => {
                if (
                    attribute.default && !attribute.deleted &&
                    attribute.name.toLowerCase() === property.name.toLowerCase()
                ) {
                    return true;
                }
            });
        }
        return false;
    }

    someAttributeIsEmpty () {
        const attributes = this.newAttributes;
        return attributes.some(attribute => {
            if (!attribute.name && !attribute.value) {
                return attribute.default;
            }
            if (attribute.default && attribute.deleted && !attribute.value) {
                return false;
            }
            if (!attribute.name || !attribute.value) {
                return true;
            }
        });
    }

    saveNewAttributes () {
        this.attributes = this.attributes
            .filter(attribute => attribute.name && attribute.value && !this.changeAttribute(attribute));
    }

    updateFeatureInfo(feature) {
        const changes = this.attributes;
        const attributes = {};
        changes.map(change => {
            change.name = change.name[0].toLowerCase() + change.name.slice(1);
            if (change.name === 'start' || change.name === 'end') {
                return;
            }
            if (change.default) {
                if (change.name in feature) {
                    if (change.deleted) {
                        delete feature[change.name];
                    } else {
                        feature[change.name] = change.value;
                    }
                } else if (change.name === feature.feature) {
                    if (change.deleted) {
                        delete feature.name;
                    } else {
                        feature.name = change.value;
                    }
                } else {
                    if (!change.deleted) {
                        attributes[change.name] = change.value;
                    }
                }
            } else {
                if (!this.changeAttribute(change)) {
                    attributes[change.name] = change.value;
                }
            }
        });
        feature.attributes = {...attributes};
        return feature;
    }

    sendNewGeneInfo (fileId, uuid, geneContent) {
        const request = {
            fileId,
            uuid,
            geneContent
        };

        return this.geneDataService.putGeneInfoEdition(request)
            .then(result => {
                if (result) {
                    this.saveError = null;
                }
            })
            .catch((error) => {
                this.saveError = [error];
                this.saveInProgress = false;
            });
    }
}
