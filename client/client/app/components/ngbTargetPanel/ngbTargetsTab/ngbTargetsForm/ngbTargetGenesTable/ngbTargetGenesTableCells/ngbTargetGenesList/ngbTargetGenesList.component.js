import controller from './ngbTargetGenesList.controller';

export default  {
    bindings: {
        value: '=',
        field: '<',
        row: '=',
        selectGene: '=',
        changeText: '=',
    },
    controller: controller.UID,
    template: require('./ngbTargetGenesList.tpl.html')
};
