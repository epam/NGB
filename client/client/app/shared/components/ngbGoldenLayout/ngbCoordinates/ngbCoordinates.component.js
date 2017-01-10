import controller from './ngbCoordinates.controller.js';

export default  {
    bindings: {
        browserId: '<',
        chromosomeName: '<',
        position: '<',
        title: '<'
    },
    controller: controller.UID,
    template: require('./ngbCoordinates.tpl.html')
};

