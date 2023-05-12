export default class ngbTargetsTableActionsController {

    static get UID() {
        return 'ngbTargetsTableActionsController';
    }

    constructor($scope, ngbTargetsTableService) {
        Object.assign(this, {$scope, ngbTargetsTableService});
    }

    get displayFilters() {
        return this.ngbTargetsTableService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbTargetsTableService.displayFilters = value;
    }
}
