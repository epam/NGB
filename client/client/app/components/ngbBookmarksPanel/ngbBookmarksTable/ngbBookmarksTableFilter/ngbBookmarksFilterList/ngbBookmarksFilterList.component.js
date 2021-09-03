import controller from './ngbBookmarksFilterList.controller';

export default  {
    bindings: {
        list: '<',
        field: '<'
    },
    controller: controller.UID,
    template: require('./ngbBookmarksFilterList.tpl.html')
};
