export default class ngbTargetsTableFilterController {
    isList = false;
    isString = false;

    static get UID() {
        return 'ngbTargetsTableFilterController';
    }

    constructor() {
        switch (this.column.field) {
            case 'name': {
                this.isString = true;
                break;
            }
            case 'owner': {
                this.isString = true;
                break;
            }
            default:
                this.isList = true;
                break;
        }
    }
}
