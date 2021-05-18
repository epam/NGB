import controller from './ngbBlastHistory.controller';

export default {
    bindings: {
        changeTab: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbBlastHistory.html'),
};
