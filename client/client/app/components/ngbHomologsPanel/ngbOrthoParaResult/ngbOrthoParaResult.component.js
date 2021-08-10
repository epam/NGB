import controller from './ngbOrthoParaResult.controller';

export default  {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    template: require('./ngbOrthoParaResult.tpl.html')
};
