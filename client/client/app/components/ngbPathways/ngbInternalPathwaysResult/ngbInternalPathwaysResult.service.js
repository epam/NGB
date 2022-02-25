export default class ngbInternalPathwaysResultService {
    constructor(genomeDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    static instance(genomeDataService, dispatcher) {
        return new ngbInternalPathwaysResultService(genomeDataService, dispatcher);
    }

    async getPathwayTree(treeConfig) {
        if(!treeConfig.id) {
            return {
                data: null,
                error: false
            };
        }
        const xml = await this.genomeDataService.loadPathwayFileById(treeConfig.id);
        try {
            const convert = require('sbgnml-to-cytoscape');
            const data = convert(xml);
            data.id = treeConfig.id;
            data.name = treeConfig.name;
            data.description = treeConfig.description;
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
}
