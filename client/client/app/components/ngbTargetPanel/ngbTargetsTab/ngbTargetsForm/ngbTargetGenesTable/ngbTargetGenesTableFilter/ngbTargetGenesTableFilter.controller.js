export default class ngbTargetGenesTableFilterController {

    static get UID() {
        return 'ngbTargetGenesTableFilterController';
    }

    isRange = false;
    isString = false;
    isList = false;

    constructor($scope, ngbTargetGenesTableService) {
        this.scope = $scope;
        this.ngbTargetGenesTableService = ngbTargetGenesTableService;
    }

    get type() {
        return this.ngbTargetGenesTableService.filterType;
    }

    $onInit() {
        const filterType = this.ngbTargetGenesTableService.getColumnFilterType(this.column.displayName);
        switch (filterType) {
            case this.type.RANGE:
                this.isRange = true;
                break;
            case this.type.RANGE,
                 this.type.PHRASE:
                this.isString = true;
                break;
            case this.type.OPTIONS:
                this.isList = true;
                break;
            default:
                this.isString = true;
                break;
        }
    }

}
