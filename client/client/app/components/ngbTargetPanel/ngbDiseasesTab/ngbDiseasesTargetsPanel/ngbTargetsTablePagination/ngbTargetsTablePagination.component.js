import controller from './ngbTargetsTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbTargetsTablePagination.tpl.html')
};
