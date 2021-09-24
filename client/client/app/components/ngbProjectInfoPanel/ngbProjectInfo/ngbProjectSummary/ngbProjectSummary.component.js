import controller from './ngbProjectSummary.controller';

export default {
    bindings: {
        heatmap: '='
    },
    controller: controller.UID,
    controllerAs: 'ctrl',
    template: require('./ngbProjectSummary.tpl.html')
};
