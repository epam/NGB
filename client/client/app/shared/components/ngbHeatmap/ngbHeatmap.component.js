import controller from './ngbHeatmap.controller';

export default {
    bindings: {
        projectId: '<',
        id: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbHeatmap.tpl.html')
};
