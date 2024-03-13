import controller from './ngbTargetGenesFilterList.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetGenesFilterList.tpl.html')
};
