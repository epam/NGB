import controller from './ngbBlastSearchAlignment.controller';

export default  {
    bindings: {
        alignment: '<',
        index: '<'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchAlignment.tpl.html')
};
