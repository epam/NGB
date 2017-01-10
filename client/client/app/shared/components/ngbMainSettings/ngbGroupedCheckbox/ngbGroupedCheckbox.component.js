import controller from './ngbGroupedCheckbox.controller';

export default {
    bindings: {
        settingItem: '='
    },
    controller: controller.UID,
    template: require('./ngbGroupedCheckbox.tpl.html')
};