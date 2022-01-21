import controller from './ngbMotifsResultsTable.controller';

export default  {
    controller: controller.UID,
    template: require('./ngbMotifsResultsTable.tpl.html'),
    bindings: {
        loading: '<'
    }
};
