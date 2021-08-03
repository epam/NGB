const UNEDITABLE = ['Start', 'End', 'Chromosome'];
const UNREMOVABLE = ['Start', 'End', 'Chromosome', 'Strand'];

export default class ngbInfoRowsController {

    static get UID() {
        return 'ngbInfoRowsController';
    }

    saveRequest = {};
    duplicate = false;

    constructor($scope, ngbFeatureInfoPanelService, $compile) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService, $compile});
    }

    get attributes () {
        return this.ngbFeatureInfoPanelService.newAttributes;
    }

    get disableAddButton () {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }

    isEditable (property) {
        return !UNEDITABLE.includes(property.name);
    }

    isRemovable (property) {
        return !UNREMOVABLE.includes(property.name);
    }

    onClickRemoveAttribute (property) {
        this.ngbFeatureInfoPanelService.removeAttribute(property);
    }

    onChangeAttribute (property) {
        if (property.name) {
            const result = this.ngbFeatureInfoPanelService.changeAttribute(property);
            this.duplicate = result;
            return result;
        }
    }

    onClickAddBtn () {
        if (this.attributes && this.attributes.length) {
            const lastAttribute = this.attributes[this.attributes.length - 1];
            if (lastAttribute.name && lastAttribute.value && !this.duplicate) {
                this.attributes.push({name: '', value: '', default: false});
            }
        }
    }
}
