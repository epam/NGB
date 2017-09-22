import controller from './ngbVariantDbSnp.controller.js';

export default {
    restrict: 'EA',
    template: require('./ngbVariantDbSnp.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings: {
        rsId: '<',
        variant: '=',
        variantRequest: '='
    }
};
