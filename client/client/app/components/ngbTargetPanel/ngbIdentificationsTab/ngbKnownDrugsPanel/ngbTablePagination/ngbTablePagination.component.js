import controller from './ngbTablePagination.controller';

export default  {
    bindings: {
        totalPages: '<',
        currentPage: '<',
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbTablePagination.tpl.html')
};
