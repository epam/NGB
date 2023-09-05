import controller from './ngbGenomicsTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbGenomicsTablePagination.tpl.html')
};
