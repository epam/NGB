export default class ngbTargetsTableFilterController {
    isList = false;
    isString = false;

    static get UID() {
        return 'ngbTargetsTableFilterController';
    }

    constructor(ngbTargetsTableService) {
        Object.assign(this, {ngbTargetsTableService});

        switch (this.column.field) {
            case 'name': {
                this.isString = true;
                break;
            }
            case 'genes': {
                this.isString = true;
                break;
            }
            default:
                this.isList = true;
                break;
        }
    }
}
