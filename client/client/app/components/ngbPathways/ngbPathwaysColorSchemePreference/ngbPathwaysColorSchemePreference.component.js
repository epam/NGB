import controller from './ngbPathwaysColorSchemePreference.controller';

export default {
    bindings: {
        scheme: '<'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbPathwaysColorSchemePreference.tpl.html'),
};
