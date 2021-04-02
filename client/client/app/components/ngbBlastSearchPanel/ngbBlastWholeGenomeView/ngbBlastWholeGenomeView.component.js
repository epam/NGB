import controller from './ngbBlastWholeGenomeView.controller';

export default {
    bindings: {
        chromosomes: '<',
    },
    restrict: 'EA',
    template: require('./ngbBlastWholeGenomeView.tpl.html'),
    controller: controller.UID,
};
