const UNEDITABLE = ['Start', 'End', 'Chromosome'];
const UNREMOVABLE = ['Start', 'End', 'Chromosome', 'Strand'];

export default class ngbInfoRowsController {

    static get UID() {
        return 'ngbInfoRowsController';
    }

    saveRequest = {};

    constructor($scope, ngbFeatureInfoPanelService, $compile) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService, $compile});
    }

    get attributes () {
        return this.ngbFeatureInfoPanelService.newAttributes;
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
        if (property.name && property.value) {
            this.ngbFeatureInfoPanelService.changeAttribute(property);
        }
    }

    onClickAddBtn () {
        if (this.attributes && this.attributes.length) {
            this.attributes.push({name: '', value: '', default: false});
        }
    }
}
