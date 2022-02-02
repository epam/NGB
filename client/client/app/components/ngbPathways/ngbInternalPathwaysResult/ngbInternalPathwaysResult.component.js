import controller from './ngbInternalPathwaysResult.controller';

export default  {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    template: require('./ngbInternalPathwaysResult.tpl.html')
};
