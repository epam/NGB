import controller from './ngbBookmarksFilterInput.controller';

export default  {
    bindings: {
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbBookmarksFilterInput.tpl.html')
};
