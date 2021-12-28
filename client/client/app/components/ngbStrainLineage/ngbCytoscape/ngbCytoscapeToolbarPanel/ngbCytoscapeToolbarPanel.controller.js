export default class ngbCytoscapeToolbarPanelController {

    constructor($timeout, $mdMenu) {
        this.$timeout = $timeout;
        this.$mdMenu = $mdMenu;
    }

    static get UID() {
        return 'ngbCytoscapeToolbarPanelController';
    }

    zoomIn() {
        this.actionsManager.zoomIn();
    }

    zoomOut() {
        this.actionsManager.zoomOut();
    }

    restoreDefault() {
        this.actionsManager.restoreDefault();
    }

    searchNode(term) {
        return this.actionsManager.searchNode(term);
    }

    onSelect(nodeId) {
        this.$mdMenu.hide();
        this.selectedNode = null;
        this.filter = null;
        this.$timeout(() => {
            this.actionsManager.selectNode(nodeId);
        }, 100);
    }

}
