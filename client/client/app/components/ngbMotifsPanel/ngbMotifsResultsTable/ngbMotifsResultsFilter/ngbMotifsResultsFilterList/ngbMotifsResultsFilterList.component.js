import controller from './ngbMotifsResultsFilterList.controller';

export default  {
    bindings: {
        field: '<',
        list: '<'
    },
    controller: controller.UID,
    template: require('./ngbMotifsResultsFilterList.tpl.html')
};
