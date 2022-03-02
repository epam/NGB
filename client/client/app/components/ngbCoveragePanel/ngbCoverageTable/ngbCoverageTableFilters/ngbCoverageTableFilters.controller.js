export default class ngbCoverageTableFiltersController {

    isRange = false;
    isList = false;

    static get UID() {
        return 'ngbCoverageTableFiltersController';
    }

    constructor(projectContext) {
        Object.assign(this, {projectContext});
        switch (this.column.field) {
            case 'chr': {
                this.isList = true;
                this.list = this.projectContext.chromosomes.map(d => d.name.toUpperCase());
                break;
            }
            case 'start': {
                this.isRange = true;
                break;
            }
            case 'end': {
                this.isRange = true;
                break;
            }
            case 'coverage':
                this.isRange = true;
                break;
            default:
                break;
        }
    }
}
