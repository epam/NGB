import controller from './ngbIdentificationChat.controller';

export default {
    bindings: {
        onClose: '<'
    },
    controller: controller.UID,
    template: require('./ngbIdentificationChat.tpl.html'),
};
