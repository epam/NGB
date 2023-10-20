export default function(ngbContextMenuBuilder) {
    return ngbContextMenuBuilder({
        controller: 'ngbDiseaseContextMenuController',
        controllerAs: '$ctrl',
        template: require('./ngbDiseaseContextMenu.tpl.html')
    });
}
