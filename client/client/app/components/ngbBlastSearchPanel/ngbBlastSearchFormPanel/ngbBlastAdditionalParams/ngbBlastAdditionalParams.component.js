import controller from './ngbBlastAdditionalParams.controller';

export default  {
    bindings: {
        additionalParams: '<',
        formObj: '='
    },
    controller: controller.UID,
    template: require('./ngbBlastAdditionalParams.tpl.html')
};
