import controller from './ngbFeatureInfoPanel.controller';

export default  {
    bindings: {
        chromosomeId: '=',
        endIndex: '=',
        name: '=',
        properties: '=',
        read: '=',
        referenceId: '=',
        startIndex: '=',
        geneId: '=',
        infoForRead: '=',
        panelTitle: '=',
        fileId: '=?',
        feature: '=?'
    },
    controller: controller.UID,
    template: require('./ngbFeatureInfoPanel.tpl.html')
};