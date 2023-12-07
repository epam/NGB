import controller from './ngbDiseasesFilterInput.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesFilterInput.tpl.html')
};