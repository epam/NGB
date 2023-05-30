import controller from './ngbTargetsFormList.controller';

export default  {
    bindings: {
        model: '@',
        index: '=',
        field: '@'
    },
    controller: controller.UID,
    template: require('./ngbTargetsFormList.tpl.html')
};
