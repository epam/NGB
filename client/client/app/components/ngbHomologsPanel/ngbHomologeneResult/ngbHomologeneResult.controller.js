import baseController from '../../../shared/baseController';

export default class ngbHomologeneResult extends baseController {

    homologene = {}
    isProgressShown = true;
    events = {
        'homologene:result:change': this.initialize.bind(this)
    };

    constructor($scope, dispatcher, ngbHomologsService, ngbHomologeneTableService) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbHomologsService,
            ngbHomologeneTableService
        });
        this.initEvents();
        this.initialize();
    }

    static get UID() {
        return 'ngbHomologeneResult';
    }

    initialize() {
        this.homologene = this.ngbHomologeneTableService.getHomologeneById(this.ngbHomologsService.currentHomologeneId);
        this.isProgressShown = false;
    }
}
