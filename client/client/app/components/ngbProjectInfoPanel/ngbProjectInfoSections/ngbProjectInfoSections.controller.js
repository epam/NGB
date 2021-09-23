import baseController from '../../../shared/baseController';

export default class ngbProjectInfoSectionsController extends baseController{
    static get UID() {
        return 'ngbProjectInfoSectionsController';
    }

    constructor($scope, dispatcher, ngbProjectInfoService) {
        super(dispatcher);
        this.ngbProjectInfoService = ngbProjectInfoService;
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
        $mdOpenMenu($event);
    }
}
