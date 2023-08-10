export default class ngbStructureViewerController {

    camera;
    currentColor;
    currentMode;

    static get UID() {
        return 'ngbStructureViewerController';
    }

    constructor($mdMenu, miewSettings, ngbStructurePanelService) {
        Object.assign(this, {ngbStructurePanelService});

        this.colorer = miewSettings.displayColors;
        this.modes = miewSettings.displayModes;
        this.menu = $mdMenu;
    }

    get descriptionDone() {
        return this.ngbStructurePanelService.descriptionDone;
    }

    get loading() {
        return this.ngbStructurePanelService.pdbDescriptionLoading;
    }

    get pdbDescriptions() {
        return this.ngbStructurePanelService.pdbDescriptions;
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
