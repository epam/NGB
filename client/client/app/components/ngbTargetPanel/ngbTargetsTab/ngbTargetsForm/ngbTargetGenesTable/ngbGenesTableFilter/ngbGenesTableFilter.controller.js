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
        if (this.column.field === 'taxId') {
            this.column.type = 'string';
        }
        if (this.column.field === 'geneName') {
            this.column.type = 'list';
        }
        if (this.column.field === 'Chromosome') {
            this.column.type = 'range';
        }
        switch (this.column.type) {
            case 'range':
                this.isRange = true;
                break;
            case 'string':
                this.isString = true;
                break;
            case 'list':
                this.isList = true;
                break;
            default:
                this.isString = true;
                break;
        }
    }

}
