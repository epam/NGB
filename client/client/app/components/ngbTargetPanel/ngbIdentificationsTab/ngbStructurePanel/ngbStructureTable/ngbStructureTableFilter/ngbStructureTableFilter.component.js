import controller from './ngbStructureTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbStructureTableFilter.tpl.html')
};
