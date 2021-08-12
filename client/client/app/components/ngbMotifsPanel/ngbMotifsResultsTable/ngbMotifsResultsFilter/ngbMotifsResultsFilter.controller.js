const FILTERED_COLUMNS = ['chromosome', 'strand', 'gene'];
export default class ngbMotifsResultsFilterController {
    showFilter = false;

    get filteredColumns () {
        return FILTERED_COLUMNS;
    }

    get positiveStrand () {
        return this.ngbMotifsPanelService.positiveStrand;
    }
    get negativeStrand () {
        return this.ngbMotifsPanelService.negativeStrand;
    }

    constructor(dispatcher, projectContext, projectDataService, ngbMotifsPanelService) {
        Object.assign(this, {
            dispatcher,
            projectContext,
            projectDataService,
            ngbMotifsPanelService
        });
        this.showFilter = this.filteredColumns.includes(this.column.field);
        this.initializeList();
        this.dispatcher.on('initialize:motif:filters', this.initializeList.bind(this));
    }

    static get UID() {
        return 'ngbMotifsResultsFilterController';
    }

    initializeList () {
        const column = this.column.field;
        switch (column) {
            case 'gene': {
                this.list = async (searchText) => {
                    if (!searchText || searchText.length < 2) {
                        return new Promise(resolve => {
                            resolve([]);
                        });
                    }
                    return await this.projectDataService.searchGeneNames(
                            this.projectContext.referenceId, searchText, 'featureName');
                };
                break;
            }
            case 'strand': {
                this.list = [this.positiveStrand, this.negativeStrand];
                break;
            }
            case 'chromosome': {
                const currentSearchType = this.ngbMotifsPanelService.currentParams.searchType;
                const chromosomeType = this.ngbMotifsPanelService.chromosomeType;
                if (currentSearchType === chromosomeType) {
                    this.list = [this.projectContext.currentChromosome.name.toUpperCase()];
                } else {
                    this.list = this.projectContext.chromosomes.map(d => d.name.toUpperCase());
                }
                break;
            }
            default:
                this.list = [];
        }
    }
}
