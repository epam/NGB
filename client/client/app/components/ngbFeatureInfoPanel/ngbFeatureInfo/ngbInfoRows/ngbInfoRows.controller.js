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

    isRemovable (property) {
        return property.attribute || !/^(start|end|chromosome|source|feature|gene|score|strand|frame)$/i.test(property.name);
    }

    valueIsEmpty (value) {
        return value === undefined || value === '' || value === null;
    }

    onClickRemoveAttribute (property) {
        this.ngbFeatureInfoPanelService.removeAttribute(property);
    }

    onChangeAttribute (property) {
        if (property.name) {
            return this.ngbFeatureInfoPanelService.changeAttribute(property);
        }
    }

    onClickAddBtn () {
        if (this.attributes && this.attributes.length) {
            this.attributes.push({
                name: '',
                value: '',
                default: false,
                attribute: true
            });
        }
    }
}
