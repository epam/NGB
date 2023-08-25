import ngbConstants from '../../../../../../constants';

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

    get selectedPdbReference() {
        return this.ngbStructurePanelService.selectedPdbReference;
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

    goFullscreen() {
        if (this.selectedPdbReference) {
            let base = ngbConstants.urlPrefix || '';
            if (base && base.length) {
                if (!base.endsWith('/')) {
                    base = base.concat('/');
                }
            }
            window.open(`${base}miew/index.html?load=${this.selectedPdbReference}`, '_blank');
        }
    }
}
