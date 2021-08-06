import controller from './ngbHomologeneResult.controller';

export default  {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    template: require('./ngbHomologeneResult.tpl.html')
};
