export default class ngbTargetsTableContextMenuController {
    static get UID() {
        return 'ngbTargetsTableContextMenuController';
    }

    constructor($scope, ngbTargetsTableContextMenu) {
        this.$scope = $scope;
        this.entity = $scope.row.entity;
        this.ngbSequenceTableContextMenu = ngbTargetsTableContextMenu;
    }
}
