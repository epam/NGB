import moment from 'moment';

function dateString (date) {
    const parsed = moment.utc(date.split(' ').join('T'));
    if (parsed.isValid()) {
        return moment(parsed.toDate()).format('D MMMM YYYY, HH:mm');
    }
    return undefined;
}

export default class ngbFeatureInfoPanelService {

    _editMode = false;
    attributes = null;
    _saveError = null;
    _historyError = null;
    _historyData = [];
    _saveInProgress = false;
    getHistoryInProgress = false;

    static instance(geneDataService) {
        return new ngbFeatureInfoPanelService(geneDataService);
    }

    constructor(geneDataService) {
        Object.assign(this, {geneDataService});
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
        const newAttributes = this.attributes;
        if (!property.default && property.name) {
            const duplicates = newAttributes.filter(attribute => {
                if (!attribute.deleted && attribute.name &&
                    attribute.name.toLowerCase() === property.name.toLowerCase()
                ) {
                    return attribute;
                }
            });
            return duplicates.length > 1;
        }
        return false;
    }

    disableSaveButton () {
        const attributes = this.newAttributes;
        const valueIsEmpty = value => value === undefined || value === null || value === '';
        return attributes.some(attribute => {
            if (!attribute.name && valueIsEmpty(attribute.value)) {
                return attribute.default;
            }
            if (attribute.default && attribute.deleted && valueIsEmpty(attribute.value)) {
                return false;
            }
            if (attribute.name && !valueIsEmpty(attribute.value)) {
                return this.changeAttribute(attribute);
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

    unsavedChanges (properties) {
        const attributes = this.attributes.filter(attribute => attribute.name || attribute.value);
        if (properties.length !== attributes.length) {
            return true;
        }
        return properties.some((property, index) => {
            return (
                property[0] !== attributes[index].name ||
                property[1] !== attributes[index].value ||
                property[2] !== attributes[index].attribute ||
                attributes[index].deleted
            );
        });
    }

    getGeneInfoHistory (fileId, uuid) {
        return new Promise((resolve) => {
            this.geneDataService.getGeneInfoHistory({fileId, uuid})
                .then(data => {
                    this.historyError = null;
                    if (data) {
                        const processed = data.map(item => ({
                            ...item,
                            dateParsed: moment.utc(item.datetime.split(' ').join('T')),
                            date: dateString(item.datetime),
                            key: `${item.username}|${dateString(item.datetime)}`
                        }));
                        const keys = [...new Set(processed.map(item => item.key))];
                        const grouped = keys
                            .map(key => processed.filter(item => item.key === key))
                            .filter(items => items.length > 0)
                            .map(items => ({
                                username: items[0].username,
                                date: items[0].date,
                                dateParsed: items[0].dateParsed,
                                changes: items,
                                key: items[0].key
                            }))
                            .sort((a, b) => b.dateParsed - a.dateParsed);
                        this.historyData = grouped;
                    }
                    resolve(true);
                })
                .catch(error => {
                    this.historyError = [error.message];
                    this.historyData = [];
                    resolve(true);
                });
        });
    }
}
