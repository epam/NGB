import controller from './ngbVariantInfo.controller.js';

export default {
    restrict: 'EA',
    template: require('./ngbVariantInfo.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings:{
        variant: '=',
        variantRequest: '='
    }
};
