import controller from './ngbOrganizeTracks.controller';

export default  {
    controller: controller.UID,
    template: require('./ngbOrganizeTracks.tpl.html'),
    bindings: {
        organizeTracks: '='
    }
};

