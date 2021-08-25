import ngbMiewController from './ngbMiew.controller';

export default  {
    template: require('./ngbMiew.tpl.html'),
    bindings: {
        pdb: '<',
        transcript: '<',
        position: '<',
        chainId: '<',
        chains: '<',
        region: '<',
        displayMode: '<',
        displayColor: '<'
    },
    controller: ngbMiewController
};
