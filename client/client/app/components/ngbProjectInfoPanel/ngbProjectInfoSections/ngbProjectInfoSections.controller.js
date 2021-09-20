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
            this.projectContext.changeState({chromosome: null});
        } else {
            this.openMenu($mdOpenMenu, $event);
        }
    }

    openMenu($mdOpenMenu, $event) {
        $event.stopPropagation();
        $mdOpenMenu($event);
    }

    onItemClick(newValue, oldValue, falseValue) {
        if (this.projectContext.currentChromosome) {
            this.projectContext.changeState({chromosome: null});
        }
        this.ngbProjectInfoService.currentMode = newValue === oldValue ? falseValue : newValue;
    }
}
