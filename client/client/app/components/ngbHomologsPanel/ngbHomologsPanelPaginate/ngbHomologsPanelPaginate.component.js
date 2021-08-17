import controller from './ngbHomologsPanelPaginate.controller';

export default  {
    bindings: {
        totalPages: '<',
        currentPage: '<',
        changePage: '&'
    },
    controller: controller.UID,
    template: require('./ngbHomologsPanelPaginate.tpl.html')
};
