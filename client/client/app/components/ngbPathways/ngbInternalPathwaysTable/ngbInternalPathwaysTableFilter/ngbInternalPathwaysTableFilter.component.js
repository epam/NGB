import controller from './ngbInternalPathwaysTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbInternalPathwaysTableFilter.tpl.html')
};
