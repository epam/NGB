export default class ngbGenesTableFilterController {

    isRange = false;
    isString = false;
    isList = false;

    constructor(projectContext, projectDataService, ngbGenesTableService) {
        switch (this.column.field) {
            case 'gene': {
                this.isList = true;
                this.list = async (searchText) => {
                    if (!searchText || searchText.length < 2) {
                        return new Promise((resolve) => {
                            resolve({
                                model: [],
                                view: []
                            });
                        });
                    }
                    const items = await projectDataService.searchGeneNames(projectContext.referenceId, searchText);
                    return new Promise((resolve) => {
                        resolve({
                            model: items.map(e => e.featureName),
                            view: items
                        });
                    });
                };
                break;
            }
            case 'type': {
                this.isList = true;
                this.list = ngbGenesTableService.geneTypeList.map(d => d.value.toUpperCase());
                break;
            }
            case 'chr': {
                this.isList = true;
                this.list = projectContext.chromosomes.map(d => d.name.toUpperCase());
                break;
            }
            case 'score':
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
