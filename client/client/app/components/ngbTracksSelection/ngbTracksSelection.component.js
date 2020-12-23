import controller from './ngbTracksSelection.controller';

export default {
    bindings: {
        browserId: '='
    },
    controller: controller.UID,
    template: require('./ngbTracksSelection.tpl.html')
};


