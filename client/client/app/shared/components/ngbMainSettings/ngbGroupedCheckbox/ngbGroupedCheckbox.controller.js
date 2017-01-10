import $ from 'jquery';

export default class groupedCheckboxController {
    static get UID() {
        return 'ngbGroupedCheckboxController';
    }

    constructor($element, $scope) {
        this.checkbox = $($element).find('md-checkbox');
        this.$scope = $scope;


        this.$scope.$on('defaultFeaturesConflict', (event, data) => {
            if (this.settingItem.name === data) {
                if (this.settingItem.byDefault.type === 'radio') {
                    this.settingItem.byDefault.model.value = this.settingItem.byDefault.model.byDefault;
                }
                if (this.settingItem.byDefault.type === 'checkbox') {
                    this.settingItem.byDefault.model = false;
                }
            }
        });
    }

    onClick() {
        if (this.settingItem.byDefault.conflicts) {
            for (const conflict of this.settingItem.byDefault.conflicts) {
                this.$scope.$root.$broadcast('defaultFeaturesConflict', conflict);
            }
        }
    }

}
