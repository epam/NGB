import controller from './ngbBlastSearchResult.controller';

export default  {
    bindings: {
        changeTab: '&'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchResult.tpl.html')
};
