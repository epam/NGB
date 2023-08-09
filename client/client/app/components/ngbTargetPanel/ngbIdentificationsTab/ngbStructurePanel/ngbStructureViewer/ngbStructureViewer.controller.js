export default class ngbStructureViewerController {

    camera;
    currentColor;
    currentMode;

    static get UID() {
        return 'ngbStructureViewerController';
    }

    constructor($scope, $mdMenu, dispatcher, miewSettings, miewContext, ngbStructurePanelService, ngbIdentificationsTabService) {
        Object.assign(this, {$scope, $mdMenu, miewContext, ngbStructurePanelService, ngbIdentificationsTabService});

        this.colorer = miewSettings.displayColors;
        this.modes = miewSettings.displayModes;
        this.menu = $mdMenu;
    }

    get selectedPdbId() {
        return this.ngbStructurePanelService.selectedPdbId;
    }

    changeDisplaySettings(name, type) {
        if (type === 'mode') {
            this.currentMode = name;
        }
        if (type === 'color') {
            this.currentColor = name;
        }
    }

    loadImage(imagePath) {
        return require(`../../../../../assets/images/${imagePath}`);
    }
}
