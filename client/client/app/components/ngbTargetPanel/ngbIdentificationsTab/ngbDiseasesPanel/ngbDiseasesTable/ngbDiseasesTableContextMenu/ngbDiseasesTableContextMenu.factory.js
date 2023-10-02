export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbDiseasesTableContextMenuController',
        controllerAs: '$ctrl',
        template: require('./ngbDiseasesTableContextMenu.tpl.html')
    });
}
