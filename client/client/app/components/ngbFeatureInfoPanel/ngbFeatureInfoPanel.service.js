export default class ngbFeatureInfoPanelService {

    _editMode = false;
    attributes = null;
    _saveError = null;
    _historyError = null;
    _historyData = null;
    _saveInProgress = false;
    getHistoryInProgress = false;
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

    get newAttributes () {
        return this.attributes;
    }

    set newAttributes (properties) {
        if (properties) {
            this.attributes = properties
                .filter(property => property[1] !== undefined)
                .map(property => ({
                    name: property[0],
                    value: property[1],
                    default: true,
                    attribute: Boolean(property[2])
                }));
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

    get historyError() {
        return this._historyError;
    }

    set historyError (error) {
        this._historyError = error;
    }

    get historyData () {
        return this._historyData;
    }

    set historyData (data) {
        this._historyData = data;
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
            return attributes.some(attribute => attribute.default &&
                !attribute.deleted &&
                attribute.name.toLowerCase() === property.name.toLowerCase()
            );
        }
        return false;
    }

    someAttributeIsEmpty () {
        const attributes = this.newAttributes;
        const valueIsEmpty = value => value === undefined || value === null || value === '';
        return attributes.some(attribute => {
            if (!attribute.name && valueIsEmpty(attribute.value)) {
                return attribute.default;
            }
            if (attribute.default && attribute.deleted && valueIsEmpty(attribute.value)) {
                return false;
            }
            return !attribute.name || valueIsEmpty(attribute.value);
        });
    }

    saveNewAttributes () {
        this.attributes = this.attributes
            .filter(attribute => attribute.name && attribute.value && !this.changeAttribute(attribute));
    }

    updateFeatureInfo(feature) {
        const updatedFeature = {attributes: {}, ...feature};
        this.attributes.forEach(change => {
            if (/^(start|end|chromosome)$/i.test(change.name)) {
                return;
            }
            const obj = change.attribute
                ? updatedFeature.attributes
                : updatedFeature;
            if (change.deleted && obj.hasOwnProperty(change.name)) {
                delete obj[change.name];
            } else if (!change.deleted) {
                obj[change.name] = Number.isNaN(Number(change.value))
                    ? change.value
                    : Number(change.value);
            }
        });
        return updatedFeature;
    }

    sendNewGeneInfo (fileId, uuid, geneContent) {
        const request = {
            fileId,
            uuid,
            geneContent
        };
        return new Promise((resolve) => {
            this.geneDataService.putGeneInfoEdition(request)
                .then(() => {
                    this.saveError = null;
                    resolve(true);
                })
                .catch((error) => {
                    this.saveError = [error.message];
                    resolve(false);
                });
        });
    }

    getGeneInfoHistory (fileId, uuid) {
        return new Promise((resolve) => {
            this.geneDataService.getGeneInfoHistory({fileId, uuid})
                .then(data => {
                    this.historyError = null;
                    if (data.length > 0) {
                        this.historyData = data;
                    }
                    resolve(true);
                })
                .catch(error => {
                    this.historyError = [error.message];
                    this.historyData = null;
                    resolve(true);
                });
        });
    }
}
