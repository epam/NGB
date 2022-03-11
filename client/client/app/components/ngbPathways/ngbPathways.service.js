const PATHWAYS_STATES = {
    INTERNAL_PATHWAYS: 'INTERNAL_PATHWAYS',
    INTERNAL_PATHWAYS_RESULT: 'INTERNAL_PATHWAYS_RESULT'
};

const PATHWAYS_STORAGE_NAME = 'pathwaysState';

export default class ngbPathwaysService {
    pathwaysServiceMap = {};

    constructor(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService,
        ngbPathwaysAnnotationService
    ) {
        Object.assign(
            this,
            {
                dispatcher,
                projectContext,
                ngbPathwaysAnnotationService
            }
        );
        this.pathwaysServiceMap = {
            [PATHWAYS_STATES.INTERNAL_PATHWAYS]: ngbInternalPathwaysTableService,
            [PATHWAYS_STATES.INTERNAL_PATHWAYS_RESULT]: ngbInternalPathwaysResultService,
        };
        this.initEvents();
        this.initState({});
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

    _currentInternalPathway;

    get currentInternalPathway() {
        return this._currentInternalPathway;
    }

    set currentInternalPathway(value) {
        this._currentInternalPathway = value;
        const loadedState = JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)) || {};
        localStorage.setItem(PATHWAYS_STORAGE_NAME, JSON.stringify({
            ...loadedState,
            internalPathway: this._currentInternalPathway
        }));
    }

    _currentState;

    get currentState() {
        return this._currentState;
    }

    set currentState(value) {
        this._currentState = value;
        const loadedState = JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)) || {};
        localStorage.setItem(PATHWAYS_STORAGE_NAME, JSON.stringify({
            ...loadedState,
            state: this._currentState
        }));
    }

    static instance(dispatcher, projectContext,
        ngbInternalPathwaysTableService, ngbInternalPathwaysResultService,
        ngbPathwaysAnnotationService) {
        return new ngbPathwaysService(dispatcher, projectContext,
            ngbInternalPathwaysTableService, ngbInternalPathwaysResultService,
            ngbPathwaysAnnotationService);
    }

    initEvents() {
        this.dispatcher.on('read:show:pathways', data => {
            this.currentSearch = data ? data.search : null;
        });
    }

    initState(loadedState) {
        loadedState = {
            // TODO: (TBD) do we need to load panel state
            // ...JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)),
            ...loadedState
        };
        this._currentState = loadedState.state;
        this._currentInternalPathway = loadedState.internalPathway;
    }

    recoverLocalState(state) {
        if (state) {
            this.initState(state.layout);
            this.ngbPathwaysAnnotationService.initState(state.annotations);
        }
    }

    getSessionState() {
        return {
            layout: JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)),
            annotations: this.ngbPathwaysAnnotationService.getSessionAnnotationList()
        };
    }

}
