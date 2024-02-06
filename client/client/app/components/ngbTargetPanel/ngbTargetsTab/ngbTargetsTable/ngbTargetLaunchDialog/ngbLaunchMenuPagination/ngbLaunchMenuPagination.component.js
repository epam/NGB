import controller from './ngbLaunchMenuPagination.controller';

export default  {
    bindings: {
        currentPage: '=',
        totalPages: '=',
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbLaunchMenuPagination.tpl.html')
};
