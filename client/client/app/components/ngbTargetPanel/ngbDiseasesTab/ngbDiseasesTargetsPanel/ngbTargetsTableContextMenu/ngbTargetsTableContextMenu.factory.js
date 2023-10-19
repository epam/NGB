export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbTargetsTableContextMenuController',
        controllerAs: '$ctrl',
        template: require('./ngbTargetsTableContextMenu.tpl.html')
    });
}
