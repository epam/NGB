import controller from './ngbDiseasesFilterList.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesFilterList.tpl.html')
};
