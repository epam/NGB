import baseController from '../../../shared/baseController';

export default class ngbProjectInfoSectionsController extends baseController{
    static get UID() {
        return 'ngbProjectInfoSectionsController';
    }

    constructor($scope, dispatcher, ngbProjectInfoService) {
        super(dispatcher);
        this.ngbProjectInfoService = ngbProjectInfoService;
    }

    openMenu($mdOpenMenu, $event) {
        $mdOpenMenu($event);
    }
}
