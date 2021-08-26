export default class ngbBookmarksTableFilterController {

    isString = false;
    isList = false;

    constructor(projectContext) {
        switch (this.column.field) {
            case 'chromosome.name': {
                this.isList = true;
                this.list = projectContext.chromosomes.map(d => d.name.toUpperCase());
                break;
            }
            case 'reference': {
                this.isList = true;
                this.list = projectContext.references.map(d => d.name.toUpperCase());
                break;
            }
            default:
                this.isString = true;
        }
    }

    static get UID() {
        return 'ngbBookmarksTableFilterController';
    }
}
