import controller from './ngbSashimiPlot.controller';

export default {
    bindings: {
        cacheService: '<',
        chromosomeName: '<',
        referenceId: '<',
        track: '<',
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    restrict: 'EA',
    template: require('./ngbSashimiPlot.tpl.html'),
};
