export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbGenesTableContextMenuController',
        controllerAs: 'ctrl',
        template: require('./ngbGenesTableContextMenu.template.html')
    });
}
