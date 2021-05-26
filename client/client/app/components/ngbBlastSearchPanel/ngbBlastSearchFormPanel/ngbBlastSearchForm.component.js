import controller from './ngbBlastSearchForm.controller';
export default {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    restrict:'E',
    template: require('./ngbBlastSearchForm.html'),
};
