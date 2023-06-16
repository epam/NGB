import controller from './ngbDiseasesTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesTableFilter.tpl.html')
};
