import controller from './ngbTracksSelection.controller';

export default {
    bindings: {
        browserId: '=',
        subMenu: '='
    },
    controller: controller.UID,
    template: require('./ngbTracksSelection.tpl.html')
};


