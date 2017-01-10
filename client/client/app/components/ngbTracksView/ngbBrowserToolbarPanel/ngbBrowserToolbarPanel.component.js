import controller from './ngbBrowserToolbarPanel.controller';

export default {
    bindings:{
        bookmarkCamera: '<',
        zoomManager: '<'
    },
    controller: controller.UID,
    template: require('./ngbBrowserToolbarPanel.tpl.html')
};