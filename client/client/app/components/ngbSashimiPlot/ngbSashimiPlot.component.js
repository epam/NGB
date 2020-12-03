import controller from './ngbSashimiPlot.controller';

export default {
    restrict: 'EA',
    template: require('./ngbSashimiPlot.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        chromosomeName: '<',
        referenceId: '<',
        tracks: '<'
    }
};



