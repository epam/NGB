export default  {
    template: require('./ngbPanelErrorList.tpl.html'),
    controller: 'ngbPanelErrorListController',
    bindings: {
        messageList: '<',
        name: '<',
    }
};
