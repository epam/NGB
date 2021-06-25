import controller from './ngbDatasetItemDownloadUrl.controller';

export default  {
    bindings: {
        id: '<',
        name: '<',
        format: '<',
        showIcon: '<'
    },
    controller: controller.UID,
    template: require('./ngbDatasetItemDownloadUrl.tpl.html')
};
