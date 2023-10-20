export default class ngbTargetsTableContextMenuController {

    targetList = [];
    loading = false;
    failedSearch = false;
    errorMessageList = null;

    static get UID() {
        return 'ngbTargetsTableContextMenuController';
    }

    constructor($scope, $timeout, dispatcher, ngbTargetsTableContextMenu, ngbTargetsTabService, targetDataService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbTargetsTableContextMenu, ngbTargetsTabService, targetDataService});
        this.entity = $scope.row.entity;
        this.searchTarget();
    }

    async searchTarget() {
        this.loading = true;
        const emptyList = [{ targetName: `No targets found for ${this.entity.target}` }];
        try {
            const list = await this.targetDataService.searchTarget(this.entity.target);
            this.targetList = list.length ? list : emptyList;
            this.failedSearch = false;
            this.errorMessageList = null;
            this.loading = false;
            this.$timeout(() => this.$scope.$apply());
        } catch (err) {
            this.targetList = emptyList;
            this.failedSearch = true;
            this.errorMessageList = err.message;
            this.loading = false;
        }
    }

    async editTarget(event, target) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbTargetsTableContextMenu.visible()) {
            this.ngbTargetsTableContextMenu.close();
        }
        if (!target.targetId) return;
        await this.ngbTargetsTabService.getTarget(target.targetId);
        this.dispatcher.emit('target:diseases:show:targets:tab');
    }
}
