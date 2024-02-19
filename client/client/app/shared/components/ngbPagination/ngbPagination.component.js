import controller from './ngbPagination.controller';

export default  {
    bindings: {
        disabled: '<',
        currentPage: '<',
        totalPages: '<',
        onChangePage: '<',
    },
    controller: controller.UID,
    template: require('./ngbPagination.html')
};
