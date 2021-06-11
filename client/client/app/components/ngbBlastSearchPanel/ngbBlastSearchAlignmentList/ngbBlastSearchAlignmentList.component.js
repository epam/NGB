import controller from './ngbBlastSearchAlignmentList.controller';

export default  {
    bindings: {
        changeState: '&'
    },
    controller: controller.UID,
    template: require('./ngbBlastSearchAlignmentList.tpl.html')
};
