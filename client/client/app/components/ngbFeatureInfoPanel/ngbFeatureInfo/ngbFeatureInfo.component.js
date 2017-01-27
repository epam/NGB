import controller from './ngbFeatureInfo.controller';

export default  {
    bindings: {
        featureId: '<',
        db: '@'
    },
    controller: controller.UID,
    template: require('./ngbFeatureInfo.tpl.html')
};