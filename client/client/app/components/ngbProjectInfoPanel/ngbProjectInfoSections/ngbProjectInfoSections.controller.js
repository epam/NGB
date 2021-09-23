import baseController from '../../../shared/baseController';

export default class ngbProjectInfoSectionsController extends baseController {
    static get UID() {
        return 'ngbProjectInfoSectionsController';
    }

    constructor($scope, dispatcher, ngbProjectInfoService, projectContext) {
        super(dispatcher);
        this.ngbProjectInfoService = ngbProjectInfoService;
        this.projectContext = projectContext;
    }

    onButtonClick($mdOpenMenu, $event) {
        if (this.projectContext.currentChromosome) {
            this.ngbProjectInfoService.currentMode = this.ngbProjectInfoService.projectInfoModeList.SUMMARY;
        } else {
            this.openMenu($mdOpenMenu, $event);
        }
    }

    get descriptionAvailable () {
        return this.ngbProjectInfoService.descriptionAvailable;
    }

    get extendedMode () {
        return this.ngbProjectInfoService.extendedMode;
    }

    setDescription (blobUrl) {
        this.ngbProjectInfoService.descriptionAvailable = true;
        this.ngbProjectInfoService.blobUrl = blobUrl;
    }

    openMenu($mdOpenMenu, $event) {
        $event.stopPropagation();
        $mdOpenMenu($event);
    }
}
