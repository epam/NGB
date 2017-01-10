export default class <%= name %>Service {

    static instance(genomeDataService, projectDataService) {
        return new <%= name %>Service(genomeDataService, projectDataService);
    }

    constructor(genomeDataService, projectDataService) {
        Object.assign(this, {genomeDataService, projectDataService});
    }

}