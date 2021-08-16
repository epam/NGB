const HOMOLOGS_STATES = {
    HOMOLOGENE: 'HOMOLOGENE',
    HOMOLOGENE_RESULT: 'HOMOLOGENE_RESULT',
    ORTHO_PARA: 'ORTHO_PARA',
    ORTHO_PARA_RESULT: 'ORTHO_PARA_RESULT'
};

export default class ngbHomologsService {
    homologsServiceMap = {};
    currentOrthoParaId;
    currentHomologeneId;
    _currentSearch;
    constructor(dispatcher, projectContext,
        ngbHomologeneTableService, ngbHomologeneResultService,
        ngbOrthoParaTableService, ngbOrthoParaResultService
    ) {
        Object.assign(
            this,
            {
                dispatcher,
                projectContext
            }
        );
        this.homologsServiceMap = {
            HOMOLOGENE: ngbHomologeneTableService,
            HOMOLOGENE_RESULT: ngbHomologeneResultService,
            ORTHO_PARA: ngbOrthoParaTableService,
            ORTHO_PARA_RESULT: ngbOrthoParaResultService
        };
        this.initEvents();

    }

    get homologsStates() {
        return HOMOLOGS_STATES;
    }

    static instance(dispatcher, projectContext,
        ngbHomologeneTableService, ngbHomologeneResultService,
        ngbOrthoParaTableService, ngbOrthoParaResultService) {
        return new ngbHomologsService(dispatcher, projectContext,
            ngbHomologeneTableService, ngbHomologeneResultService,
            ngbOrthoParaTableService, ngbOrthoParaResultService);
    }

    get currentSearch() {
        return this._currentSearch;
    }

    set currentSearch(value) {
        this._currentSearch = value;
    }

    initEvents() {
        this.dispatcher.on('read:show:homologs', data => {
            this.currentSearch = data.search;
        });
    }
}
