export default class ngbInternalPathwaysTableFilterController {

    isRange = false;
    isString = false;
    isList = false;

    constructor(ngbInternalPathwaysTableService) {
        switch (this.column.field) {
            case 'organisms': {
                this.isList = true;
                this.list = async () => new Promise((resolve) => {
                    resolve({
                        model: ngbInternalPathwaysTableService.speciesList.map(e => e.taxId.toString()),
                        view: ngbInternalPathwaysTableService.speciesList
                    });
                });
                break;
            }
        }
    }

    static get UID() {
        return 'ngbInternalPathwaysTableFilterController';
    }
}
