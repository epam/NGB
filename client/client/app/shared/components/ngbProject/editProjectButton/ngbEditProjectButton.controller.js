import ngbProjectTabsController from '../ngbProjectTabs.controller';
import angular from 'angular';


export default class ngbEditProjectButtonController {
    static get UID() {
        return 'ngbEditProjectButtonController';
    }

    /** @ngInject */
    constructor(dispatcher, $mdDialog, ngbProjectService) {
        this._dispatcher = dispatcher;
        this._mdDialog = $mdDialog;
        this._ngbProjectService = ngbProjectService;
    }

    INIT() {

    }

    editProject(ev) {
        ev.stopPropagation();
        this._mdDialog.show({
            template: require('../ngbProjectTabs.tpl.html'),
            controller: ngbProjectTabsController,
            controllerAs: 'ctrl',
            locals : {
                emittedEvent: 'ngbProject:edit',
                projectId: this.projectId
            },
            parent: angular.element(document.body),
            targetEvent: ev,
            clickOutsideToClose:true
        })
            .then(function() {
                //$scope.status = 'You said the information was "' + answer + '".';
            }, function() {
                //$scope.status = 'You cancelled the dialog.';
            });
    }

}