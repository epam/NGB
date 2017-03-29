import controller from './ngbVariantsFilterRange.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbVariantsFilterRange.tpl.html')
};
