import controller from './ngbGenesFilterList.controller';

export default  {
    bindings: {
        list: '<',
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbGenesFilterList.tpl.html')
};
