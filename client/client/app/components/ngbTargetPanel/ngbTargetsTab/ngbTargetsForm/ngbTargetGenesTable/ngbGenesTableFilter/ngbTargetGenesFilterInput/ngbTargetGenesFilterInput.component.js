import controller from './ngbTargetGenesFilterInput.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetGenesFilterInput.tpl.html')
};
