import controller from './ngbHeatmap.controller';

export default {
    bindings: {
        projectId: '<',
        id: '<',
        heatmapClass: '<',
        checkResize: '<',
        renderOnChange: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbHeatmap.tpl.html')
};
