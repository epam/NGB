import controller from './ngbMotifsResultsFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbMotifsResultsFilter.tpl.html')
};
