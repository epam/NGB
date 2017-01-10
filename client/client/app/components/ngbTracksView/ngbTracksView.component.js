import controller from './ngbTracksView.controller';

export default {
    restrict: 'EA',
    template: require('./ngbTracksView.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        brushStart: '<',
        brushEnd: '<',
        position: '<',
        chromosomeName: '<',
        browserId: '<'
    }
};


