import controller from './ngbVariantVisualizer.controller';

export default {
    restrict: 'EA',
    template: require('./ngbVariantVisualizer.tpl.html'),
    controller: controller.UID,
    controllerAs: 'ctrl',
    bindings:{
        variant: '=',
        variantRequest: '='
    }
};
