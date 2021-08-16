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

    initEvents() {
    }

    async getCurrentSearch() {
        let data = {};
        if (this.currentSearch) {
            data = this._formatServerToClient(await this.genomeDataService.getHomologsSearch(this.currentSearch));
        }
        return {request: data};
    }

    async getCurrentSearchResult() {
        let data = {};
        if (this.currentResultId) {
            data = this._formatServerToClient(await this.genomeDataService.getHomologsSearch(this.currentResultId));
        }
        if (data) {
            this._isFailureResults = data.isFailure = data.state === 'FAILED';
        } else {
            this._isFailureResults = true;
        }
        return data;
    }

    createSearchRequest(searchRequest) {
        searchRequest.organismsArray = searchRequest.organisms ? searchRequest.organisms.map(o => o.taxid) : [];
        return this.genomeDataService.createHomologsSearch(this._formatClientToServer(searchRequest)).then(data => {
            if (data && data.id) {
                this.currentSearch = data.id;
                localStorage.removeItem('homologeneRequest');
                this.currentSearch = null;
            }
            return data;
        });
    }

    _formatServerToClient(search) {
        const result = {
            id: search.id,
            title: search.title,
            algorithm: search.algorithm,
            db: search.database ? search.database.id : undefined,
            dbName: search.database ? search.database.name : '',
            dbSource: search.database ? search.database.source : undefined,
            dbType: search.database ? search.database.type : undefined,
            tool: search.executable,
            sequence: search.query,
            state: search.status,
            reason: search.statusReason,
            options: search.options,
            submitted: search.createdDate
        };
        if (search.excludedOrganisms) {
            result.organisms = search.excludedOrganisms.map(oId => ({taxid: oId.taxId, scientificname: oId.scientificName}));
            result.isExcluded = true;
        } else {
            result.organisms = search.organisms ? search.organisms.map(oId => ({taxid: oId.taxId, scientificname: oId.scientificName})) : [];
            result.isExcluded = false;
        }
        if (search.parameters) {
            result.maxTargetSeqs = search.parameters.max_target_seqs;
            result.threshold = search.parameters.evalue;
        }
        return result;
    }

    _formatClientToServer(search) {
        const result = {
            title: search.title || '',
            algorithm: search.algorithm,
            databaseId: search.db,
            executable: search.tool,
            query: search.sequence,
            parameters: {}
        };
        if (search.isExcluded) {
            result.excludedOrganisms = search.organismsArray || [];
        } else {
            result.organisms = search.organismsArray || [];
        }
        if (search.maxTargetSeqs) {
            result.parameters.max_target_seqs = search.maxTargetSeqs;
        }
        if (search.threshold) {
            result.parameters.evalue = search.threshold;
        }
        if (search.options) {
            result.options = search.options;
        }
        return result;
    }
}
