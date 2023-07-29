import controller from './ngbChat.controller';

export default {
    bindings: {
        messages: '<',
        loading: '<',
        messageLoading: '<',
        onSendMessage: '<',
        placeholder: '<',
        readOnly: '<',
        disabled: '<'
    },
    controller: controller.UID,
    template: require('./ngbChat.tpl.html'),
};
