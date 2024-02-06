import controller from './ngbTargetGenesFilterRange.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetGenesFilterRange.tpl.html')
};