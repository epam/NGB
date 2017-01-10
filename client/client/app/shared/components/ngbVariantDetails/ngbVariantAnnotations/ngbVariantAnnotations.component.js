import controller from './ngbVariantAnnotations.controller';

export default {
    restrict: 'EA',
    template: require('./ngbVariantAnnotations.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings:{
        variant: '=',
        variantRequest: '='
    }
};

