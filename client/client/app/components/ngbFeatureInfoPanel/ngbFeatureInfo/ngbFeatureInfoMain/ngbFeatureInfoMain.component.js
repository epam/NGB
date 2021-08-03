import controller from './ngbFeatureInfoMain.controller';

export default  {
    bindings: {
        chromosomeId: '=',
        endIndex: '=',
        name: '=',
        properties: '=',
        read: '=',
        referenceId: '=',
        startIndex: '=',
        infoForRead: '=',
        fileId: '=?',
        feature: '=?'
    },
    controller: controller.UID,
    template: require('./ngbFeatureInfoMain.tpl.html')
};