import controller from './ngbDiseasesTargetsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesTargetsTableFilter.tpl.html')
};
