import controller from './ngbInfoRows.controller';

export default {
    template: require('./ngbInfoRows.tpl.html'),
    controller: controller.UID,
    bindings: {
        properties: '<',
        editable: '=?'
    }
};
