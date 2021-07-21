import controller from './ngbGenesFilterRange.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbGenesFilterRange.tpl.html')
};
