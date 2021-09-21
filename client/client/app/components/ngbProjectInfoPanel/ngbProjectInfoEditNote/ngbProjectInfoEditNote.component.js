import controller from './ngbProjectInfoEditNote.controller';

export default {
    template: require('./ngbProjectInfoEditNote.tpl.html'),
    controller: controller.UID,
    bindings: {
        note: '<'
    }
};
