export default {
    template: require('./projectGeneral.tpl.html'),
    bindings: {
        emittedEvent: '<event',
        projectId: '<?'
    },
    controller: 'projectGeneralController',
    controllerAs: 'ctrl'
};