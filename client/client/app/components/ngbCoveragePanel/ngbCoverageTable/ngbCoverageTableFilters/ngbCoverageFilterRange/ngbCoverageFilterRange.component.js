import controller from './ngbCoverageFilterRange.controller';

export default {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbCoverageFilterRange.tpl.html')
};
