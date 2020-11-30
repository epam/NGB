export default class ngbWigOpacityPreferenceController {
    static get UID() {
        return 'ngbWigOpacityPreferenceController';
    }

    constructor($scope, $mdDialog, dispatcher, source, config) {
        Object.assign(this, {
            $mdDialog,
            $scope,
            config,
            dispatcher,
            source,
        });

        this.opacity = 30;
    }
    save() {
        this.$mdDialog.hide();
    }

    close() {
        this.$mdDialog.hide();
    }
}
