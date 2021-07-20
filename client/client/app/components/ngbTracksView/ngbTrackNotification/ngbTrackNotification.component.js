import controller from './ngbTrackNotification.controller';

export default {
    bindings:{
        notification: '=',
        trackInstance: '='
    },
    controller: controller.UID,
    restrict: 'E',
    template: require('./ngbTrackNotification.tpl.html')
};
