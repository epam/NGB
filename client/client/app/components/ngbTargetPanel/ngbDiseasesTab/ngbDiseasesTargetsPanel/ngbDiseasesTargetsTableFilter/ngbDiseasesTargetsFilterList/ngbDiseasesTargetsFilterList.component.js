import controller from './ngbDiseasesTargetsFilterList.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesTargetsFilterList.tpl.html')
};
