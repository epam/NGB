export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbSequenceTableContextMenuController',
        controllerAs: '$ctrl',
        template: require('./ngbSequenceTableContextMenu.tpl.html')
    });
}
