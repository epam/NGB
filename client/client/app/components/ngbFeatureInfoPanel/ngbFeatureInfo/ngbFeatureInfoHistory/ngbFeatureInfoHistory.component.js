import controller from './ngbFeatureInfoHistory.controller';

export default  {
    bindings: {
        data: '=',
    },
    controller: controller.UID,
    template: require('./ngbFeatureInfoHistory.tpl.html')
};
