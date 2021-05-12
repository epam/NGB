import controller from './ngbBlastWholeGenomeView.controller';

export default {
    bindings: {
        chromosomes: '<',
        blastResult:'<',
    },
    restrict: 'EA',
    template: require('./ngbBlastWholeGenomeView.tpl.html'),
    controller: controller.UID,
};
