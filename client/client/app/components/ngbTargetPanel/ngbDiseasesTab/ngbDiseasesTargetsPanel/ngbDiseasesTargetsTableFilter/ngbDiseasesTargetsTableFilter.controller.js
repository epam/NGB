export default class ngbDiseasesTargetsTableFilterController {

    isString = false;
    isList = false;

    static get UID() {
        return 'ngbDiseasesTargetsTableFilterController';
    }

    constructor() {
        switch (this.column.field) {
            case 'homologues': {
                this.isList = true;
                break;
            }
            default:
                this.isString = true;
                break;
        }
    }
}
