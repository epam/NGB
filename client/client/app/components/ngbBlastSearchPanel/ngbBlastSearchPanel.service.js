const BLAST_STATES = {
    HISTORY: 'HISTORY',
    RESULT: 'RESULT',
    SEARCH: 'SEARCH',
    ALIGNMENT: 'ALIGNMENT'
};

const blastpSupportedTypes = ['GENE', 'MRNA', 'CDS'];
const wrongFeatureTypeMessage = 'Only GENE, MRNA or CDS FeatureType are supported!';

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
    constructor(dispatcher, projectContext, bamDataService, projectDataService,
        ngbBlastSearchFormConstants, genomeDataService, geneDataService) {
        Object.assign(
            this,
            {
                dispatcher,
                projectContext,
                bamDataService,
                projectDataService,
                ngbBlastSearchFormConstants,
                genomeDataService,
                geneDataService
            }
        );
        this.currentTool = this.ngbBlastSearchFormConstants.BLAST_TOOLS[0];
        this.initEvents();
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
    _isRepeat = false;

    get totalPagesCountHistory() {
        return this._totalPagesCountHistory;
    }

    set totalPagesCountHistory(value) {
        this._totalPagesCountHistory = value;
    }

    get blastStates() {
        return BLAST_STATES;
    }

    static instance(dispatcher, projectContext, bamDataService, projectDataService,
        ngbBlastSearchFormConstants, genomeDataService, geneDataService) {
        return new ngbBlastSearchService(dispatcher, projectContext, bamDataService,
            projectDataService, ngbBlastSearchFormConstants, genomeDataService, geneDataService);
    }

    initEvents() {
        this.dispatcher.on('read:show:blast', data => {
            this.currentSearchId = null;
            this.currentTool = data.tool;
        });
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

            const blastSettings = this.projectContext.getTrackDefaultSettings('blast_settings');
            const maxQueryLengthProperty = 'query_max_length';
            const MAX_SIZE = blastSettings &&
            blastSettings.hasOwnProperty(maxQueryLengthProperty) &&
            !Number.isNaN(Number(blastSettings[maxQueryLengthProperty]))
                ? Number(blastSettings[maxQueryLengthProperty])
                : Infinity;
            const size = endIndex - startIndex;

            if (size > MAX_SIZE) {
                return {
                    error: `Query maximum length (${MAX_SIZE}bp) exceeded`
                };
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
        } else if (!blastpSupportedTypes.includes(featureType)) {
            return {
                error: wrongFeatureTypeMessage
            };
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

    get isRepeat() {
        return this._isRepeat;
    }

    set isRepeat(value) {
        this._isRepeat = value;
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
        if (!data.db) {
            data.db = [];
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
                title: data.title
            };
        } else {
            this._isFailureResults = true;
            this._cutCurrentResult = null;
        }
        return data;
    }

    createSearchRequest(searchRequest, additionalParams) {
        searchRequest.organismsArray = searchRequest.organisms ? searchRequest.organisms.map(o => o.taxid) : [];
        searchRequest.dbArray = searchRequest.db ? searchRequest.db.map(o => o.id) : [];
        return this.projectDataService.createBlastSearch(this._formatClientToServer(searchRequest, additionalParams)).then(data => {
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
        if (!search || !searchResult || search.dbSource !== 'NCBI') {
            return Promise.resolve(undefined);
        }
        const {sequenceAccessionVersion, sequenceId, taxId} = searchResult;
        const {dbType} = search;
        const id = sequenceAccessionVersion || sequenceId;
        const db = search && dbType && /^protein$/i.test(dbType)
            ? 'PROTEIN'
            : 'NUCLEOTIDE';
        return new Promise((resolve) => {
            this.projectDataService.getFeatureCoordinates(id, db, taxId)
                .then((result) => {
                    if (result.error) {
                        resolve(undefined);
                    } else {
                        resolve(result);
                    }
                })
                .catch(() => resolve(undefined));
        });
    }

    _formatServerToClient(search) {
        const result = {
            id: search.id,
            title: search.title,
            algorithm: search.algorithm,
            db: search.database || [],
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
        if (result.options) {
            result.parameters = this._parseSearchOptions(result.options);
        }
        return result;
    }

    _formatClientToServer(search, additionalParams) {
        const result = {
            title: search.title || '',
            algorithm: search.algorithm,
            databaseId: search.dbArray,
            executable: search.tool,
            query: search.sequence
        };
        if (search.isExcluded) {
            result.excludedOrganisms = search.organismsArray || [];
        } else {
            result.organisms = search.organismsArray || [];
        }
        let options = this.stringifySearchOptions(additionalParams);
        if (search.options) {
            options = `${search.options.trim()} ${options}`;
        }
        if (options) {
            result.options = options.trim();
        }
        return result;
    }

    _parseSearchOptions(options) {
        const isKey = item => /^-.*[^\d.]+/.test(item);
        const params = {};
        const regex = /([^"\s]+)|"(?:\\"|[^"])+"/g;
        const splitString = options.match(regex).map(p => p.trim());
        for (let index = 0; index < splitString.length; index++) {
            if (isKey(splitString[index])) {
                if (!splitString[index + 1] || isKey(splitString[index + 1])) {
                    params[splitString[index].substring(1)] = 'true';
                } else {
                    params[splitString[index].substring(1)] = splitString[index + 1].replace(/^"|"$/g, '');
                    index++;
                }
            }
        }
        return params;
    }

    stringifySearchOptions(options) {
        let result = '';
        Object.keys(options || {}).forEach(key => {
            if (options[key].value === null || options[key].value === undefined || options[key].value === '') {
                return;
            }
            let value;
            switch (true) {
                case options[key].isNumber: {
                    result += `-${key} ${options[key].value} `;
                    break;
                }
                case options[key].isBoolean: {
                    value = options[key].value ? 'true' : 'false';
                    result += `-${key} ${value} `;
                    break;
                }
                case options[key].isFlag: {
                    if (options[key].value) {
                        result += `-${key} `;
                    }
                    break;
                }
                default: {
                    value = options[key].value || '';
                    value = value.replace(/^'|'$/g, '"');
                    const end = value[value.length - 1] === '"' ? value.length - 1 : value.length,
                        start = value[0] === '"' ? 1 : 0;
                    // escaping quotes inside string
                    value = `"${value.substring(start, end).replace(/\\([\s\S])|(")/, '\\$1$2')}"`;
                    result += `-${key} ${value} `;
                }
            }
        });
        return result;
    }
}
