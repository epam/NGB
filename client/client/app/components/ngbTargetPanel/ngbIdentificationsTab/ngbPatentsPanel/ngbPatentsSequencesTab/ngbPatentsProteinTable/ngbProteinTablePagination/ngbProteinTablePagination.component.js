import controller from './ngbProteinTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbProteinTablePagination.tpl.html')
};
