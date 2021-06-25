import controller from './ngbBlastSearchResult.controller';

export default  {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchResult.tpl.html')
};
