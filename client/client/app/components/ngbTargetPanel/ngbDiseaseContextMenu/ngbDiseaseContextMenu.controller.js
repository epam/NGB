export default class ngbDiseaseContextMenuController {

    static get UID() {
        return 'ngbDiseaseContextMenuController';
    }

    constructor($scope, dispatcher, ngbDiseaseContextMenu) {
        this.$scope = $scope;
        this.dispatcher = dispatcher;
        this.entity = $scope.row.entity;
        this.ngbDiseaseContextMenu = ngbDiseaseContextMenu;
    }

    viewDiseaseDetails(event) {
        event.preventDefault();
        event.stopPropagation();
        this.dispatcher.emit('target:identification:show:diseases:tab', this.entity.disease);
        if (this.ngbDiseaseContextMenu.visible()) {
            this.ngbDiseaseContextMenu.close();
        }
    }
}
