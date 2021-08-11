import baseController from '../../../shared/baseController';

export default class ngbOrthoParaResult extends baseController {

    isProgressShown = true;
    searchResultError = null;
    events = {
        'orthoPara:result:change': this.initialize.bind(this)
    };

    constructor($scope, dispatcher, ngbHomologsService, ngbOrthoParaResultService) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbHomologsService,
            ngbOrthoParaResultService
        });
        this.initEvents();
        this.initialize();
    }

    static get UID() {
        return 'ngbOrthoParaResult';
    }

    initialize() {
        this.searchResultError = this.ngbOrthoParaResultService.searchResultTableError;
        this.isProgressShown = false;
    }
}
