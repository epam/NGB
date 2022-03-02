import controller from './ngbCoverageFilterList.controller';

export default {
    bindings: {
        field: '<',
        list: '<'
    },
    controller: controller.UID,
    template: require('./ngbCoverageFilterList.tpl.html')
};
