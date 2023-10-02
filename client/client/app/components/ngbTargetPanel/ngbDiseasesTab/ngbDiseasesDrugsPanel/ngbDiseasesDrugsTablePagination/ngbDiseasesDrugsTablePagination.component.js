import controller from './ngbDiseasesDrugsTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbDiseasesDrugsTablePagination.tpl.html')
};
