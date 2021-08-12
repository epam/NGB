import controller from './ngbMotifsTablePagination.controller';

export default  {
    bindings: {
        totalPages: '<',
        currentPage: '<',
        changePage: '&'
    },
    controller: controller.UID,
    template: require('./ngbMotifsTablePagination.tpl.html')
};
