export default class ngbInternalPathwaysResultService {

    currentReferenceId = null;

    constructor(genomeDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    static instance(genomeDataService, dispatcher) {
        return new ngbInternalPathwaysResultService(genomeDataService, dispatcher);
    }

    async getPathwayTreeById(id) {
        if(!id) {
            return {
                data: null,
                error: false
            };
        }
        const xml = require(`./xml/${id}.xml`);
        try {
            const convert = require('sbgnml-to-cytoscape');
            const data = convert(xml);
            if (data) {
                data.id = id;
                return {
                    data: {
                        name: id,
                        tree: data
                    },
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
