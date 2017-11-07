import controller from './ngbBlatSearch.controller';

export default  {
    bindings: {
        isProgressShown: '='
    },
    controller: controller.UID,
    template: require('./ngbBlatSearch.tpl.html')
};
