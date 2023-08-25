import controller from './ngbSequencesTablePagination.controller';

export default  {
    bindings: {
        onChangePage: '&',
    },
    controller: controller.UID,
    template: require('./ngbSequencesTablePagination.tpl.html')
};
