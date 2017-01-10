export default class ngbEditProjectButtonController {
    static get UID() {
        return 'ngbDeleteProjectButtonController';
    }

    /** @ngInject */
    constructor(dispatcher, $mdDialog, projectDataService) {
        this._dispatcher = dispatcher;
        this._mdDialog = $mdDialog;
        this._projectDataService = projectDataService;
    }

    INIT() {

    }

    deleteProject(ev) {
        const projectDataService = this._projectDataService;
        const projectId = this.projectId;
        const dispatcher = this._dispatcher;
        const spliceIndex = this.index;
        ev.stopPropagation();
        const confirm = this._mdDialog.confirm()
            .title(`Would you like to delete project "${  this.projectName }"?`)
            .textContent('This action can not be revert.')
            .targetEvent(ev)
            .ok('Yes')
            .cancel('No');
        this._mdDialog.show(confirm).then(function() {
            projectDataService.deleteProject(projectId);
            dispatcher.emitGlobalEvent('ngbProject:projects:change', {spliceIndex});
        }, function() {
            //$scope.status = 'You decided to keep your debt.';
        });

    }

}