import controller from './ngbViewAction.controller';

export default  {
    bindings: {
        icon: '<',
        event: '<',
        label: '<'
    },
    controller: controller.UID,
    template: require('./ngbViewAction.tpl.html')
};
