import controller from './ngbChemicalTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbChemicalTablePagination.tpl.html')
};
