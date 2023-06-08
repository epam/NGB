import controller from './ngbTargetsFilterList.controller';

export default {
    bindings: { column: '<' },
    controller: controller.UID,
    template: require('./ngbTargetsFilterList.tpl.html')
};
