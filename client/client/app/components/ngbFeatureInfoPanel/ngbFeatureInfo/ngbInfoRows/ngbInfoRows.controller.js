const UNEDITABLE = ['Start', 'End', 'Chromosome'];
const UNREMOVABLE = ['Start', 'End', 'Chromosome', 'Strand'];

export default class ngbInfoRowsController {

    static get UID() {
        return 'ngbInfoRowsController';
    }

    saveRequest = {};

    constructor($scope, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService});
    }

    get attributes () {
        return this.ngbFeatureInfoPanelService.newAttributes;
    }

    isEditable (property) {
        return !UNEDITABLE.includes(property[0]);
    }

    isRemovable (property) {
        return !UNREMOVABLE.includes(property[0]);
    }

    onClickRemoveAttribute (property) {
        this.ngbFeatureInfoPanelService.removeAttribute(property);
    }

    onChangeAttribute (property) {
        this.ngbFeatureInfoPanelService.changeAttribute(property);
    }

    onClickAddBtn () {
        if (this.properties && this.properties.length) {
            const div = document.createElement('div');
            const newAttribute = require('./ngbInfoRows.newRow.tpl.html');
            div.innerHTML = newAttribute;
            document.querySelector('.general-information-form')
                .append(div);
        }
    }
}
