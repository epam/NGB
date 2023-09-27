import controller from './ngbTargetsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbTargetsTableFilter.tpl.html')
};
