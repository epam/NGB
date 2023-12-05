import controller from './ngbTargetsTableMenu.controller';

export default  {
    bindings: {
        entity: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetsTableMenu.tpl.html')
};
