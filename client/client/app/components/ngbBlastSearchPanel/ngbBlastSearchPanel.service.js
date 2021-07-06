const BLAST_STATES = {
    HISTORY: 'HISTORY',
    RESULT: 'RESULT',
    SEARCH: 'SEARCH',
    ALIGNMENT: 'ALIGNMENT'
};

function scientificNameSorter(a, b) {
    const aName = (a.scientificname || '').toLowerCase();
    const bName = (b.scientificname || '').toLowerCase();
    if (aName < bName) {
        return -1;
    }
    if (aName > bName) {
        return 1;
    }
    return 0;
}

export default class ngbBlastSearchService {
    static instance(dispatcher, bamDataService, projectDataService, ngbBlastSearchFormConstants, genomeDataService, geneDataService) {
        return new ngbBlastSearchService(dispatcher, bamDataService, projectDataService, ngbBlastSearchFormConstants, genomeDataService, geneDataService);
    }

    _detailedRead = null;
    _totalPagesCountHistory = 0;
    _currentResultId = null;
    _currentSearchId = null;
    _currentTool = null;
    _isFailureResults = true;
    _isEmptyResults = true;
    _cutCurrentResult = null;
    _currentAlignmentObject = {};

    get totalPagesCountHistory() {
        return this._totalPagesCountHistory;
    }

    set totalPagesCountHistory(value) {
        this._totalPagesCountHistory = value;
    }

    get blastStates() {
        return BLAST_STATES;
    }

    constructor(dispatcher, bamDataService, projectDataService, ngbBlastSearchFormConstants, genomeDataService, geneDataService) {
        Object.assign(
            this,
            {
                dispatcher,
                bamDataService,
                projectDataService,
                ngbBlastSearchFormConstants,
                genomeDataService,
                geneDataService
            }
        );
        this.currentTool = this.ngbBlastSearchFormConstants.BLAST_TOOLS[0];
    }

    async getOrganismList(term, selectedOrganisms = []) {
        const selectedIds = selectedOrganisms.map(value => value.taxid);
        const organismList = await this.projectDataService.getOrganismList(term);
        return organismList
            .map(o => ({
                scientificname: o.scientificName,
                commonname: o.commonName,
                taxid: o.taxId
            }))
            .filter(value => !selectedIds.includes(value.taxid))
            .sort(scientificNameSorter);
    }

    async getBlastDBList(type) {
        return await this.projectDataService.getBlastDBList(type);
    }

    async getNucleotideSequence(searchRequest) {
        const {
            startIndex,
            endIndex,
            chromosomeId,
            referenceId: id
        } = searchRequest;
        if (
            startIndex &&
            endIndex &&
            chromosomeId &&
            id
        ) {
            const size = endIndex - startIndex;
            const MAX_SIZE = 100 * 1000;
            if (size > MAX_SIZE) {
                return {};
            }
            const blockSize = 10 * 1024; // 10kb;
            const count = Math.ceil(size / blockSize);
            const payloads = [];
            for (let p = 0; p < count; p++) {
                const start = startIndex + blockSize * p;
                const end = Math.min(endIndex, startIndex + blockSize * (p + 1) - 1);
                payloads.push({
                    startIndex: start,
                    endIndex: end,
                    scaleFactor: 1,
                    chromosomeId,
                    id
                });
            }
            try {
                const result = await this.genomeDataService.loadReferenceTrack(payloads);
                if (result && result.blocks && result.blocks.length) {
                    return {
                        sequence: result.blocks.map(block => block.text).join('')
                    };
                }
            } catch (e) {
                // eslint-disable-next-line
                console.warn('Error fetching sequence', e.message);
                return {
                    error: e.message
                };
            }
        }
        return {};
    }

    async getAminoAcidSequence(searchRequest) {
        const {
            startIndex,
            endIndex,
            chromosomeId,
            referenceId,
            id,
            name: featureId,
            feature: featureType = ''
        } = searchRequest;
        const aminoAcidsPayload = {
            trackQuery: {
                id,
                chromosomeId,
                endIndex,
                startIndex,
                scaleFactor: 1
            },
            referenceId,
            featureId,
            featureType: featureType.toUpperCase()
        };
        if (
            id &&
            referenceId &&
            chromosomeId &&
            featureId
        ) {
            try {
                const result = await this.geneDataService.getAminoAcids(aminoAcidsPayload);
                return {
                    sequence: result
                };
            } catch (e) {
                // eslint-disable-next-line
                console.warn('Error fetching sequence', e.message);
                return {
                    error: e.message
                };
            }
        }
        return {};
    }

    async getSequence() {
        const searchRequest = JSON.parse(localStorage.getItem('blastSearchRequest')) || null;
        if (searchRequest) {
            switch (searchRequest.source) {
                case 'bam': {
                    try {
                        return await this.bamDataService.loadRead(searchRequest);
                    } catch (e) {
                        return {
                            error: e.message
                        };
                    }
                }
                default:
                case 'gene': {
                    if (searchRequest.aminoAcid) {
                        return await this.getAminoAcidSequence(searchRequest);
                    }
                    return await this.getNucleotideSequence(searchRequest);
                }
            }
        }
        return undefined;
    }


    set currentSearchId(currentSearchId) {
        this._currentSearchId = currentSearchId;
    }

    get currentResultId() {
        return this._currentResultId;
    }

    set currentResultId(currentResultId) {
        this._currentResultId = currentResultId;
    }

    get currentTool() {
        return this._currentTool;
    }

    set currentTool(tool) {
        this._currentTool = tool;
    }

    set isEmptyResults(value) {
        this._isEmptyResults = value;
    }

    get canDownload() {
        return !this._isEmptyResults && !this._isFailureResults;
    }

    get cutCurrentResult() {
        return this._cutCurrentResult;
    }

    async getCurrentSearch() {
        let data = {};
        let error;
        if (this._currentSearchId) {
            data = this._formatServerToClient(await this.projectDataService.getBlastSearch(this._currentSearchId));
        } else {
            const newSearch = await this.getSequence();
            if (newSearch) {
                data.sequence = newSearch.sequence;
                error = newSearch.error;
            }
            if (this.currentTool) {
                data.tool = this.currentTool;
            }
        }
        if (!data.organisms) {
            data.organisms = [];
        }
        return {request: data, error};
    }

    popCurrentAlignmentObject() {
        const result = {...this._currentAlignmentObject};
        this._currentAlignmentObject = {};
        return result;
    }

    setCurrentAlignmentObject(alignment) {
        this._currentAlignmentObject = alignment;
    }

    async getCurrentSearchResult() {
        let data = {};
        if (this.currentResultId) {
            data = this._formatServerToClient(await this.projectDataService.getBlastSearch(this.currentResultId));
        }
        if (data) {
            this._isFailureResults = data.isFailure = data.state === 'FAILED';
            this._cutCurrentResult = {
                id: data.id,
                tool: data.tool,
                db: data.db,
                dbName: data.dbName,
                dbSource: data.dbSource,
                dbType: data.dbType,
                title: data.title
            };
        } else {
            this._isFailureResults = true;
            this._cutCurrentResult = null;
        }
        return data;
    }

    createSearchRequest(searchRequest) {
        searchRequest.organismsArray = searchRequest.organisms ? searchRequest.organisms.map(o => o.taxid) : [];
        return this.projectDataService.createBlastSearch(this._formatClientToServer(searchRequest)).then(data => {
            if (data && data.id) {
                this.currentSearchId = data.id;
                localStorage.removeItem('blastSearchRequest');
                this.currentSearchId = null;
                this.currentTool = this.ngbBlastSearchFormConstants.BLAST_TOOLS[0];
            }
            return data;
        });
    }

    fetchFeatureCoords(searchResult, search) {
       return this.projectDataService.getNCBIFeatureCoordinates(searchResult, search);
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
