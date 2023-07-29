import controller from './controller';

export default {
    restrict: 'E',
    bindings: {
        onClose: '<',
        placement: '<'
    },
    transclude: {
        title: 'ngbFloatingPanelTitle',
        body: 'ngbFloatingPanelContent'
    },
    controller,
    template: require('./template.html'),
};
