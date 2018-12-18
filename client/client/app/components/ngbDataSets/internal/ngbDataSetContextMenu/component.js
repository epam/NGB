export default function(ngbContextMenu) {
    return ngbContextMenu({
        controller: 'ngbDataSetContextMenuController',
        controllerAs: 'contextMenu',
        template: require('./template.html')
    });
}
