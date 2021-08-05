import controller from './ngbFeatureInfoMain.controller';

export default  {
    bindings: {
        chromosomeId: '=',
        endIndex: '=',
        name: '=',
        properties: '=',
        editable: '=',
        read: '=',
        referenceId: '=',
        startIndex: '=',
        infoForRead: '=',
        fileId: '=?',
        feature: '=?',
        uuid: '=?',
    },
    controller: controller.UID,
    template: require('./ngbFeatureInfoMain.tpl.html')
};
