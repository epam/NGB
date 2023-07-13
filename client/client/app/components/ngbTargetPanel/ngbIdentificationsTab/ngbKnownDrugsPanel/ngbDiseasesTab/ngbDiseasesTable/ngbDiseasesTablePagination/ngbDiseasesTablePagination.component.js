import controller from './ngbDiseasesTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbDiseasesTablePagination.tpl.html')
};
