export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbHomologsTableContextMenuController',
        controllerAs: 'ctrl',
        template: require('./ngbHomologsTableContextMenu.tpl.html')
    });
}
