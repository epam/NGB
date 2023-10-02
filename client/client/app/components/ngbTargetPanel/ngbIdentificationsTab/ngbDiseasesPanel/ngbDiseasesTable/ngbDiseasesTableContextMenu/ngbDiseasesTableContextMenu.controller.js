export default class ngbDiseasesTableContextMenuController {

    static get UID() {
        return 'ngbDiseasesTableContextMenuController';
    }

    constructor($scope, dispatcher, ngbDiseasesTableContextMenu) {
        this.$scope = $scope;
        this.dispatcher = dispatcher;
        this.entity = $scope.row.entity;
        this.ngbDiseasesTableContextMenu = ngbDiseasesTableContextMenu;
    }

    viewDiseaseDetails(event) {
        event.preventDefault();
        event.stopPropagation();
        this.dispatcher.emit('target:identification:show:diseases:tab', this.entity.disease);
        if (this.ngbDiseasesTableContextMenu.visible()) {
            this.ngbDiseasesTableContextMenu.close();
        }
    }
}
