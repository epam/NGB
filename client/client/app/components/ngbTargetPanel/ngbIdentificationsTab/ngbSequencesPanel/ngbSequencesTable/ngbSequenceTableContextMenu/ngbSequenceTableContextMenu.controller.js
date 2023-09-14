export default class ngbSequenceTableContextMenuController {

    static get UID() {
        return 'ngbSequenceTableContextMenuController';
    }

    constructor($scope, ngbSequenceTableContextMenu) {
        this.$scope = $scope;
        this.ngbSequenceTableContextMenu = ngbSequenceTableContextMenu;
    }

    copyToClipboard(event) {
        event.preventDefault();
        event.stopPropagation();
        if (this.ngbSequenceTableContextMenu.visible()) {
            this.ngbSequenceTableContextMenu.close();
        }
    }
}
