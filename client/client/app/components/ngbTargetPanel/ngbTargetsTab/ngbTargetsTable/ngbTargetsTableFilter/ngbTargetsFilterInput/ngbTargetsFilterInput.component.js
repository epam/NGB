import controller from './ngbTargetsFilterInput.controller';

export default {
    bindings: { column: '<' },
    controller: controller.UID,
    template: require('./ngbTargetsFilterInput.tpl.html')
};
