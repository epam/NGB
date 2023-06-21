import controller from './ngbDrugsTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbDrugsTableFilter.tpl.html')
};
