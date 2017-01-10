import controller from './ngbVariantsTable.controller';

export default  {
    bindings: {
        isProgressShown: '='
    },
    controller: controller.UID,
    template: require('./ngbVariantsTable.tpl.html')
};
