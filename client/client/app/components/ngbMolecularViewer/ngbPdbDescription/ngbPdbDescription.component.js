import controller from './ngbPdbDescription.controller.js';

export default  {
    template: require('./ngbPdbDescription.tpl.html'),
    bindings: {
        pdb: '<',
        loadDone: '=',
        description: '=',
        pdbList: '<',
        chainId: '=',
    },
    controller: controller.UID
};