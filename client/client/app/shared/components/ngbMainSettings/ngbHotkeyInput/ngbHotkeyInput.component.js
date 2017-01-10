import controller from './ngbHotkeyInput.controller';

export default {
    bindings: {
        hotkeys: '=',
        item: '='
    },
    controller: controller.UID,
    template: require('./ngbHotkeyInput.tpl.html')
};