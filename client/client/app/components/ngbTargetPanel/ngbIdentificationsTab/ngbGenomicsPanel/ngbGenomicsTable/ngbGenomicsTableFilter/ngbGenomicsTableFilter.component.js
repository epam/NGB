import controller from './ngbGenomicsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbGenomicsTableFilter.tpl.html')
};
