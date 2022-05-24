export default class ngbFeatureInfoMainActionsController {

    static get UID() {
        return 'ngbFeatureInfoMainActionsController';
    }

    constructor($scope, dispatcher, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbFeatureInfoPanelService});
        const onClickCancelBtn = () => this.onClickCancelBtn();
        this.dispatcher.on('feature:info:changes:cancel', onClickCancelBtn);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('feature:info:changes:cancel', onClickCancelBtn);
        });
    }

    get editMode() {
        return this.ngbFeatureInfoPanelService.editMode;
    }

    get disableSave () {
        return this.ngbFeatureInfoPanelService.disableSaveButton();
    }

    get saveInProgress () {
        return this.ngbFeatureInfoPanelService.saveInProgress;
    }

    onClickEditBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:edit:click');
    }

    onClickCancelBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:cancel:click');
    }

    onClickSaveBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:save:click');
    }

    onClickAddBtn () {
        this.dispatcher.emitSimpleEvent('feature:info:add:click', 'sequencePanel');
    }
}
