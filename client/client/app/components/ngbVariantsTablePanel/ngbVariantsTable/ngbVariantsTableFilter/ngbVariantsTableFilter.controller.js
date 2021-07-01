export default class ngbVariantsTableFilterController {

    static get UID() {
        return 'ngbVariantsTableFilterController';
    }

    projectContext;

    isRange = false;
    isString = false;
    isFlag = false;
    isList = false;
    list = [];

    scope;
    dispatcher;

    constructor($scope, dispatcher, projectContext, projectDataService) {
        this.scope = $scope;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        switch (this.column.field) {
            case 'startIndex': this.isRange = true; break;
            case 'variationType': {
                this.isList = true;
                this.list = ['SNV', 'BND', 'DEL', 'INS', 'DUP', 'INV'];
            } break;
            case 'geneNames': {
                this.isList = true;
                this.list = async (searchText) => {
                    if (!searchText || searchText.length < 2) {
                        return new Promise((resolve) => {
                            resolve([]);
                        });
                    }
                    return await projectDataService.searchGeneNames(this.projectContext.referenceId, searchText, 'featureName');
                };
            } break;
            case 'chrName': {
                this.isList = true;
                this.list = this.projectContext.chromosomes.map(d => d.name.toUpperCase());

            } break;
            default: {
                const [vcfField] = this.projectContext.vcfInfo.filter(f => f.name.toLowerCase() === this.column.field.toLowerCase());
                if (vcfField) {
                    switch (vcfField.type.toLowerCase()) {
                        case 'flag': this.isFlag = true; break;
                        case 'integer':
                        case 'float': {
                            this.isRange = true;
                        } break;
                        default: {
                            this.isString = true;
                        } break;
                    }
                }
            } break;
        }
    }

}
