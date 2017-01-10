import controller from './ngbBookmarksTable.controller';

export default  {
    bindings: {
        searchPattern: '='
    },
    template: require('./ngbBookmarksTable.tpl.html'),
    controller: controller.UID
};
