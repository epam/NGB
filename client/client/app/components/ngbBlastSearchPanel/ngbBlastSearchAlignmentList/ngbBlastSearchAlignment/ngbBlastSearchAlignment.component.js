import controller from './ngbBlastSearchAlignment.controller';

export default  {
    bindings: {
        alignment: '<',
        index: '<',
        searchResult: '<',
        search: '<'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchAlignment.tpl.html')
};
