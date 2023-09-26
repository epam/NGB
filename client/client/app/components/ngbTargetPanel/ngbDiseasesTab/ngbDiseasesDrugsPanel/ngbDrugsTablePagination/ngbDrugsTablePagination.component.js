import controller from './ngbDrugsTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbDrugsTablePagination.tpl.html')
};
