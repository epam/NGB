const PATHWAYS_STATES = {
    KEGG: 'KEGG',
    INTERNAL_PATHWAYS: 'INTERNAL_PATHWAYS',
    INTERNAL_PATHWAYS_RESULT: 'INTERNAL_PATHWAYS_RESULT'
};

export default class ngbPathwaysService {
    pathwaysServiceMap = {};
    currentInternalPathway;

    constructor(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService
    ) {
        Object.assign(
            this,
            {
                dispatcher,
                projectContext
            }
        );
        this.pathwaysServiceMap = {
            [PATHWAYS_STATES.INTERNAL_PATHWAYS]: ngbInternalPathwaysTableService,
            [PATHWAYS_STATES.INTERNAL_PATHWAYS_RESULT]: ngbInternalPathwaysResultService,
        };
        this.initEvents();
    }

    _currentSearch;

    get currentSearch() {
        return this._currentSearch;
    }

    set currentSearch(value) {
        this._currentSearch = value;
    }

    get pathwaysStates() {
        return PATHWAYS_STATES;
    }

    static instance(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService) {
        return new ngbPathwaysService(dispatcher, projectContext,
            ngbInternalPathwaysTableService, ngbInternalPathwaysResultService);
    }

    initEvents() {
        this.dispatcher.on('read:show:pathways', data => {
            this.currentSearch = data ? data.search : null;
        });
    }

}
