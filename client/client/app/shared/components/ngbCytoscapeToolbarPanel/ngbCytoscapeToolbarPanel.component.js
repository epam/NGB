import controller from './ngbCytoscapeToolbarPanel.controller';

export default {
    bindings:{
        actionsManager: '<'
    },
    controller: controller.UID,
    template: require('./ngbCytoscapeToolbarPanel.tpl.html')
};
