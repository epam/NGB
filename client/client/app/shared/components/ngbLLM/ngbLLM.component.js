import controller from './ngbLLM.controller';

export default {
    bindings: {
        models: '<',
        modelOptions: '=',
        onChange: '<',
        iconSize: '<',
        hideIcon: '<'
    },
    controller: controller.UID,
    template: require('./ngbLLM.tpl.html'),
};
