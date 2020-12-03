import controller from './ngbSashimiPlot.controller';

export default {
    bindings: {
        chromosomeName: '<',
        referenceId: '<',
        tracks: '<'
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbSashimiPlot.tpl.html'),
};
