import controller from './ngbVariantsFilterCheckbox.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbVariantsFilterCheckbox.tpl.html')
};
