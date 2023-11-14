import controller from './ngbDiseasesTargetsFilterInput.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDiseasesTargetsFilterInput.tpl.html')
};