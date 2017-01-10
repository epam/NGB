import ngbProjectTabsController from '../ngbProjectTabs.controller';

import angular from 'angular';

export default class ngbCreateProjectController {
    static get UID() {
        return 'ngbCreateProjectController';
    }

    /** @ngInject */
    constructor(dispatcher, $mdDialog, ngbProjectService) {
        this._dispatcher = dispatcher;
        this._mdDialog = $mdDialog;
        this._ngbProjectService = ngbProjectService;
    }

    INIT() {

    }

    createProject(ev) {
        this._mdDialog.show({
            template: require('../ngbProjectTabs.tpl.html'),
            controller: ngbProjectTabsController,
            controllerAs: 'ctrl',
            parent: angular.element(document.body),
            targetEvent: ev,
            locals : {
                emittedEvent: 'ngbProject:create:new',
                projectId: ''
            },
            clickOutsideToClose:true
        })
            .then(function() {
                //$scope.status = 'You said the information was "' + answer + '".';
            }, function() {
                //$scope.status = 'You cancelled the dialog.';
            });
    }

}