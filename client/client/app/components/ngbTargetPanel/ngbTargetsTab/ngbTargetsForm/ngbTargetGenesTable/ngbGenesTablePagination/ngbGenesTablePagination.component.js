import controller from './ngbGenesTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbGenesTablePagination.tpl.html')
};
