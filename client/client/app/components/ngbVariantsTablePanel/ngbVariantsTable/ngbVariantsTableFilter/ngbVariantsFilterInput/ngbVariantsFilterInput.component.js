import controller from './ngbVariantsFilterInput.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbVariantsFilterInput.tpl.html')
};
