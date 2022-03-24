const DATABASE_SOURCES = {
    CUSTOM: 'CUSTOM',
    BIOCYC: 'BIOCYC',
    COLLAGE: 'COLLAGE'
};

const LOCAL_STORAGE_KEY = 'internal-pathways-state';

export default class ngbInternalPathwaysResultService {
    constructor(genomeDataService, projectDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.projectDataService = projectDataService;
    }

    static instance(genomeDataService, projectDataService, dispatcher) {
        return new ngbInternalPathwaysResultService(genomeDataService, projectDataService, dispatcher);
    }

    get localStorageKey() {
        return LOCAL_STORAGE_KEY;
    }

    async getPathwayTree(treeConfig) {
        if (!treeConfig.id) {
            return {
                data: null,
                error: false
            };
        }
        const rawData = await this.genomeDataService.loadPathwayFileById(treeConfig.id);
        try {
            let data;
            if (treeConfig.source === DATABASE_SOURCES.COLLAGE) {
                const parsedData = JSON.parse(rawData);
                data = parsedData.elements;
                data.style = parsedData.style;
            } else {
                data = require('sbgnml-to-cytoscape')(rawData);
            }
            data.id = treeConfig.id;
            data.name = treeConfig.name;
            data.description = treeConfig.description;
            data.source = treeConfig.source;
            if (treeConfig.taxId) {
                data.attrs = {taxId: treeConfig.taxId};
            }
            if (data) {
                return {
                    data: data,
                    error: false
                };
            } else {
                return {
                    data: null,
                    error: false
                };
            }
        } catch (e) {
            return {
                data: null,
                error: e.message
            };
        }
    }

    async getNavigationToChromosomeInfo(context, taxId) {
        const [reference] = (context.references || [])
            .filter(reference.species && Number(reference.species.taxId) === Number(taxId));
        const referenceId = reference ? reference.id : undefined;
        if (!referenceId) {
            return null;
        }

        return {
            referenceId
        };
    }


    async searchGenes(referenceId, geneId) {
        const data = await this.projectDataService.searchGenes(
            referenceId,
            geneId
        );
        return data ? data.entries ? data.entries[0] : null : null;
    }
}
