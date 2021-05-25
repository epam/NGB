import controller from './ngbBlastHistory.controller';

export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbBlastHistory.tpl.html'),
};
