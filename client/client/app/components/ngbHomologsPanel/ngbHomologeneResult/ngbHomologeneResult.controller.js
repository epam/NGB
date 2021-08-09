import baseController from '../../../shared/baseController';

export default class ngbHomologeneResult extends baseController {

    isProgressShown = true;
    searchResultError = null;
    events = {
        'homologene:result:change': this.initialize.bind(this)
    };

    constructor($scope, dispatcher, ngbHomologeneResultService) {
        super();
        Object.assign(this, {
            $scope,
            dispatcher,
            ngbHomologeneResultService
        });
        this.initEvents();
        this.initialize();
    }

    static get UID() {
        return 'ngbHomologeneResult';
    }

    initialize() {
        this.searchResultError = this.ngbHomologeneResultService.searchResultTableError;
        this.isProgressShown = false;
    }
}
