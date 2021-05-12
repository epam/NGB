import controller from './ngbBlastSearchForm.controller';
export default {
    bindings:{
        sequence: '=',
    },
    controller: controller.UID,
    restrict:'EA',
    template: require('./ngbBlastSearchForm.html'),
};
