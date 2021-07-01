import controller from './ngbGenesTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbGenesTableFilter.tpl.html')
};
