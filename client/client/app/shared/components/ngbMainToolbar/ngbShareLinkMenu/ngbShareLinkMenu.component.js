import controller from './ngbShareLinkMenu.controller';

export default  {
    bindings: {
        url: '='
    },
    controller: controller.UID,
    template: require('./ngbShareLinkMenu.tpl.html')
};

