import controller from './ngbTargetGenesTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetGenesTableFilter.tpl.html')
};
