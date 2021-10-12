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

    get menuPositionMode() {
        if (this.subMenu) {
            return 'cascade target';
        }
        return 'target-left target';
    }

    onButtonClick($mdOpenMenu, $event) {
        if (this.projectContext.currentChromosome) {
            this.ngbProjectInfoService.currentMode = this.ngbProjectInfoService.projectInfoModeList.SUMMARY;
        } else {
            this.openMenu($mdOpenMenu, $event);
        }
    }

    openMenu($mdOpenMenu, $event) {
        $event.stopPropagation();
        $mdOpenMenu($event);
    }
}
