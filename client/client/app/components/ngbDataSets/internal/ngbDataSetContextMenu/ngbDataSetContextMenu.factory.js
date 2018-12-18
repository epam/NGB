export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbDataSetContextMenuController',
        controllerAs: 'ctrl',
        template: require('./ngbDataSetContextMenu.template.html')
    });
}
