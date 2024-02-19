import controller from './ngbPatentsSummary.controller';

export default {
    bindings: {
        search: '<',
    },
    controller: controller.UID,
    template: require('./ngbPatentsSummary.tpl.html'),
};