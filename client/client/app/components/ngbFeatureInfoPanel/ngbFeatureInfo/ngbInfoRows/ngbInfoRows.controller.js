const UNEDITABLE = ['Start', 'End', 'Chromosome'];
const UNREMOVABLE = ['Start', 'End', 'Chromosome', 'Strand'];

export default class ngbInfoRowsController {

    static get UID() {
        return 'ngbInfoRowsController';
    }

    saveRequest = {};

    constructor($scope) {
        Object.assign(this, {$scope});
    }

    isEditable (property) {
        return !UNEDITABLE.includes(property[0]);
    }

    isRemovable (property) {
        return !UNREMOVABLE.includes(property[0]);
    }

    onClickRemoveProperty (property) {
        const index = this.properties.indexOf(property);
        this.properties.splice(index, 1);
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
