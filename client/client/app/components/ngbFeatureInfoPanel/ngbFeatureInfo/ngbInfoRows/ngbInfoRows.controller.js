import angular from 'angular';

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

    isDefault (property) {
        const isInclude = this.properties.some(element => element[0] === property.name);
        return isInclude;
    }

    onClickRemoveAttribute (property) {
        this.ngbFeatureInfoPanelService.removeAttribute(property);
    }

    onChangeAttribute (property) {
        if (property.name && property.value) {
            this.ngbFeatureInfoPanelService.changeAttribute(property);
        }
    }

    onChangeNewAttrubuteName (name) {
        this.ngbFeatureInfoPanelService.onChangeNewAttrubuteName(name);
    }

    onChangeNewAttrubuteValue (value) {
        this.ngbFeatureInfoPanelService.onChangeNewAttrubuteValue(value);
    }

    onClickRemoveNewAttribute () {
        const element = angular.element(document.querySelector('.attribute-draft'));
        this.ngbFeatureInfoPanelService.onClickRemoveNewAttribute();
        element.remove();
        this.$scope.$destroy();
    }

    onClickAddBtn () {
        if (this.attributes && this.attributes.length) {
            if (this.ngbFeatureInfoPanelService.attributeDraft) {
                if (this.ngbFeatureInfoPanelService.isAttributeValid()) {
                    this.ngbFeatureInfoPanelService.addAttribute();
                }
            }
            if (!this.ngbFeatureInfoPanelService.attributeDraft) {
                const form = angular.element(document.querySelector('.general-information-form'));
                const newAttribute = this.$compile('<ngb-info-new-row></ngb-info-new-row>')(this.$scope.$new());
                form.append(newAttribute);
                this.ngbFeatureInfoPanelService.attributeDraft = {};
            }
        }
    }
}
