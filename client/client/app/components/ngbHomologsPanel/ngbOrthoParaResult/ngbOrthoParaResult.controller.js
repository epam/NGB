import baseController from '../../../shared/baseController';

export default class ngbOrthoParaResult extends baseController {

    orthoPara = {};
    isProgressShown = true;
    events = {
        'orthoPara:result:change': this.initialize.bind(this)
    };

    constructor($scope, dispatcher, ngbHomologsService, ngbOrthoParaTableService, ngbOrthoParaResultService) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbHomologsService,
            ngbOrthoParaTableService,
            ngbOrthoParaResultService
        });
        this.initEvents();
        this.initialize();
    }

    static get UID() {
        return 'ngbOrthoParaResult';
    }

    initialize() {
        this.orthoPara = this.ngbOrthoParaTableService.getOrthoParaById(this.ngbHomologsService.currentOrthoParaId);
        this.isProgressShown = false;
    }
}
