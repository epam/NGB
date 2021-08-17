import controller from './ngbHomologsDomains.controller';

export default {
    bindings: {
        domains: '<',
        homologLength: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbHomologsDomains.tpl.html'),
};
