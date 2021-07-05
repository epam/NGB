export default class ngbGenesTableFilterController {

    isRange = false;
    isString = false;

    constructor() {
        switch (this.column.field) {
            case 'start':
            case 'end':
                this.isRange = true;
                break;
            default:
                this.isString = true;
        }
    }

    static get UID() {
        return 'ngbGenesTableFilterController';
    }
}
