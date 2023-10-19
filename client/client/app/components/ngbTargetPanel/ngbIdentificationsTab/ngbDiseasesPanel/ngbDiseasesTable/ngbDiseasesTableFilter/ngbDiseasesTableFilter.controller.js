export default class ngbDiseasesTableFilterController {

    isString = false;
    isList = false;

    static get UID() {
        return 'ngbDiseasesTableFilterController';
    }

    constructor() {
        switch (this.column.field) {
            case 'target': {
                this.isList = true;
                break;
            }
            default:
                this.isString = true;
                break;
        }
    }
}
