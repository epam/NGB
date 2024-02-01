export default class ngbGenesTableFilterController {

    static get UID() {
        return 'ngbGenesTableFilterController';
    }

    isRange = false;
    isString = false;
    isList = false;
    list = [];

    constructor($scope) {
        this.scope = $scope;
    }

    $onInit() {
        switch (this.column.type) {
            case 'range':
                this.isRange = true;
                break;
            case 'string':
                this.isString = true;
                break;
            case 'geneNames':
                this.isList = true;
                break;
            default:
                this.isRange = true;
                break;
        }
    }

}
