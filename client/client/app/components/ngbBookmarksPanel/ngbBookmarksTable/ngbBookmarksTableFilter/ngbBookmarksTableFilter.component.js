import controller from './ngbBookmarksTableFilter.controller';

export default  {
    bindings: {
        column: '<'
    },
    controller: controller.UID,
    template: require('./ngbBookmarksTableFilter.tpl.html')
};
