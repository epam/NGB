import controller from './ngbCoverageTableFilters.controller';

export default {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbCoverageTableFilters.tpl.html')
};
