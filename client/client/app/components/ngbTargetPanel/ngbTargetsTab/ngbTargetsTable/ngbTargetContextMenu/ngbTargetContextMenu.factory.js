export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbTargetContextMenuController',
        controllerAs: '$ctrl',
        template: require('./ngbTargetContextMenu.tpl.html')
    });
}
