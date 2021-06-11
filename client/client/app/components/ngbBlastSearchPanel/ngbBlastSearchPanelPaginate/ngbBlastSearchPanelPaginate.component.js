import controller from './ngbBlastSearchPanelPaginate.controller';

export default  {
    bindings: {
        totalPages: '<',
        currentPage: '<',
        changePage: '&'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchPanelPaginate.tpl.html')
};
