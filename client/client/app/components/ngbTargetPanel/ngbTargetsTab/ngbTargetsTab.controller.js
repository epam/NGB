export default class ngbTargetsTabController {

    static get UID() {
        return 'ngbTargetsTabController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTabService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbTargetsTabService});

        const refreshTable = this.refreshTable.bind(this);
        dispatcher.on('target:launch:failed', refreshTable);
        dispatcher.on('target:launch:failed:refresh', refreshTable);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:launch:failed', refreshTable);
            dispatcher.removeListener('target:launch:failed:refresh', refreshTable);
        });
    }

    refreshTable() {
        this.$timeout(() => this.$scope.$apply());
    }

    get isTableMode() {
        return this.ngbTargetsTabService.isTableMode;
    }

    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    get isEditMode() {
        return this.ngbTargetsTabService.isEditMode;
    }
}
