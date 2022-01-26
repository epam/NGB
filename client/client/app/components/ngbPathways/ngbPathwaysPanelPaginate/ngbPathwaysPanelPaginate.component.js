import controller from './ngbPathwaysPanelPaginate.controller';

export default  {
    bindings: {
        totalPages: '<',
        currentPage: '<',
        changePage: '&'
    },
    controller: controller.UID,
    template: require('./ngbPathwaysPanelPaginate.tpl.html')
};
