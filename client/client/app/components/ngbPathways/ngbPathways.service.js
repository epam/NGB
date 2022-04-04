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
        this.report();
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

    get routeInfo() {
        if (this.currentInternalPathway) {
            return JSON.stringify(this.currentInternalPathway);
        }
        return null;
    }

    set routeInfo(routeInfo) {
        try {
            if (routeInfo) {
                const internalPathway = JSON.parse(routeInfo);
                const state = {
                    internalPathway,
                    state: this.pathwaysStates.INTERNAL_PATHWAYS_RESULT
                };
                this.initState(state);
                this.dispatcher.emit('load:pathways', state);
            }
        } catch (_) {
            this.currentState = this.pathwaysStates.INTERNAL_PATHWAYS;
        }
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
            this.currentSearch = data || null;
        });
    }

    initState(loadedState) {
        loadedState = {
            ...JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)),
            ...loadedState
        };
        this._currentState = loadedState.state;
        this.currentInternalPathway = loadedState.internalPathway;
    }

    recoverLocalState(state) {
        if (state) {
            this.initState(state.layout);
            this.ngbPathwaysAnnotationService.initState(state.annotations);
            this.dispatcher.emit('load:pathways', state.layout);
        }
    }

    getSessionState() {
        return {
            layout: JSON.parse(localStorage.getItem(PATHWAYS_STORAGE_NAME)),
            annotations: this.ngbPathwaysAnnotationService.getSessionAnnotationList()
        };
    }

    report() {
        this.dispatcher.emitGlobalEvent('route:change');
    }


}
