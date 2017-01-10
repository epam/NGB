export default {
    template: require('./projectTracks.tpl.html'),
    bindings: {
        emittedEvent: '<event',
        projectId: '<?'
    },
    controller: 'projectTracksController',
    controllerAs: 'ctrl'
};