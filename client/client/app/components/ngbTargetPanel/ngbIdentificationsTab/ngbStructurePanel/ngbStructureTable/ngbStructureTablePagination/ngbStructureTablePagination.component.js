import controller from './ngbStructureTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbStructureTablePagination.tpl.html')
};
