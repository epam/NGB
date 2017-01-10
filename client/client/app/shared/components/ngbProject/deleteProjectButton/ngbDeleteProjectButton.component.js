export default {
    template: require('./ngbDeleteProjectButton.tpl.html'),
    bindings: {
        projectId: '<',
        projectName: '<',
        index: '<'
    },
    controller: 'ngbDeleteProjectButtonController',
    controllerAs: 'ctrl'
};