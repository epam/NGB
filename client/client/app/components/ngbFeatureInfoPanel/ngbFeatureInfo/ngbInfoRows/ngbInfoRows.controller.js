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

    get disableAddButton () {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }

    isEditable (property) {
        return property.attribute || !/^(start|end|chromosome|)$/i.test(property.name);
    }

    isDuplicate (attribute) {
        return this.ngbFeatureInfoPanelService.isDuplicate(attribute);
    }

    valueIsEmpty (value) {
        return value === undefined || value === '' || value === null;
    }

    onClickRemoveAttribute (property) {
        this.ngbFeatureInfoPanelService.removeAttribute(property);
    }

    onClickAddBtn () {
        if (this.attributes && this.attributes.length) {
            this.attributes.push({
                name: '',
                value: '',
                default: false,
                attribute: true,
                isInput: true
            });
        }
    }
}
