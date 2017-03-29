import controller from './ngbVariantsFilterList.controller';

export default  {
    bindings: {
        list: '<',
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbVariantsFilterList.tpl.html')
};
