import controller from './ngbInternalPathwaysFilterList.controller';

export default  {
    bindings: {
        list: '<',
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbInternalPathwaysFilterList.tpl.html')
};
