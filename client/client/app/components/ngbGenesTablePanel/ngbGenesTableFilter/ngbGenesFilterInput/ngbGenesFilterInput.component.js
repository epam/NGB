import controller from './ngbGenesFilterInput.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbGenesFilterInput.tpl.html')
};
