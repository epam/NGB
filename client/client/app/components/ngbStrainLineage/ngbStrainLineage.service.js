export default class ngbStrainLineageService {

    constructor(projectDataService, geneDataService, dispatcher) {
        this.dispatcher = dispatcher;
        this.projectDataService = projectDataService;
    }

    static instance(projectDataService, geneDataService, dispatcher) {
        return new ngbStrainLineageService(projectDataService, geneDataService, dispatcher);
    }

}
