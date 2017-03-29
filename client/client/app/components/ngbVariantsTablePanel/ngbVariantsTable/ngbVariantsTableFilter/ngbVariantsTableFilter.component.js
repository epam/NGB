import controller from './ngbVariantsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbVariantsTableFilter.tpl.html')
};
