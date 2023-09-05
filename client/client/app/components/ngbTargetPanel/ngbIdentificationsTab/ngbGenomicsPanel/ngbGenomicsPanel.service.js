import {calculateColor} from '../../../../shared/utils/calculateColor';

const PAGE_SIZE = 10;

const capitalize = (string) => {
    return string.charAt(0).toUpperCase() + string.slice(1).toLowerCase();
};

export default class ngbGenomicsPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _alignment = null;
    _totalCount = 0;
    _homologsData = null;
    _currentPage = 1;
    _totalPages = 0;
    _emptyResults = false;

    get alignment() {
        return this._alignment;
    }

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    get totalCount() {
        return this._totalCount;
    }
    get homologsData() {
        return this._homologsData;
    }
    get currentPage () {
        return this._currentPage;
    }
    set currentPage (value) {
        this._currentPage = value ;
    }
    get totalPages() {
        return this._totalPages;
    }
    get emptyResults() {
        return this._emptyResults;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        return new ngbGenomicsPanelService(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        Object.assign(this, {ngbTargetPanelService, targetDataService, genomeDataService});
        dispatcher.on('target:identification:changed', this.resetData.bind(this));
        this.getHomologene();
    }

    get allGenes() {
        const { interest = [], translational = [] } = this.ngbTargetPanelService.identificationTarget || {};
        return [...interest, ...translational];
    }

    get genesOfInterest () {
        const { interest = [] } = this.ngbTargetPanelService.identificationTarget || {};
        return [...interest];
    }

    get translationalGenes () {
        const { translational = [] } = this.ngbTargetPanelService.identificationTarget || {};
        return [...translational];
    }

    setAlignment(data) {
        const [target, query] = data;
        const getName = (item) => {
            return item.split(' ')[0];
        };
        this._alignment = {
            targetName: getName(target.name),
            targetTooltip: target.name,
            targetSequence: target.baseString,
            targetStart: 1,
            targetEnd: target.baseString.length,
            queryName: getName(query.name),
            queryTooltip: query.name,
            querySequence: query.baseString,
            queryStart: 1,
            queryEnd: query.baseString.length,
        };
    }

    getTargetAlignment(targetId, sequenceIds) {
        return new Promise(resolve => {
            this.targetDataService.getTargetAlignment(targetId, sequenceIds)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    if (data && data.length === 2) {
                        this.setAlignment(data);
                    } else {
                        this._alignment = null;
                    }
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._alignment = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    getGenomicsResults() {
        const genomicsResults = this._homologsData;
        if (!genomicsResults || !genomicsResults.length) return [];
        const start = (this.currentPage - 1) * this.pageSize;
        const end = this.currentPage * this.pageSize;
        const results = genomicsResults.slice(start, end);
        return results;
    }

    setHomologsData(data) {
        const totalCount = (data.totalCount || 0);
        this._totalCount = this._totalCount ? (this._totalCount + totalCount) : data.totalCount;
        const items = data.items || [];
        if (items.length) {
            const maxGroupLengths = items.reduce((lengths, item) => {
                lengths[item.groupId] = item.homologs.reduce((max, homolog) => Math.max(max, homolog.protLen), 0);
                return lengths;
            }, {});
            const getHomologData = (item) => {
                return (item.homologs || []).map(h => ({
                    target: this.genesOfInterest[0].chip,
                    species: h.speciesScientificName,
                    'homology type': capitalize(item.type),
                    homologue: h.symbol || `id: ${h.geneId}`,
                    geneId: h.geneId,
                    'homology group': item.groupId,
                    'protein': h.title,
                    aa: h.protLen,
                    domains: {
                        domains: (h.domains || []).map(d => ({
                            id: d.pssmId,
                            start: d.begin,
                            end: d.end,
                            name: d.cddName,
                            color: calculateColor(d.cddName)
                        })),
                        homologLength: h.protLen,
                        maxHomologLength: maxGroupLengths[item.groupId],
                        accession_id: h.accession_id
                    }
                }));
            };
            const homologsData = items.reduce((acc, item) => (
                [...acc, ...getHomologData(item)]
            ), []);
            this._homologsData = [
                ...(this._homologsData || []),
                ...homologsData
            ];
        }
    }

    setHomologeneData(data) {
        const totalCount = (data.totalCount || 0);
        this._totalCount = this._totalCount ? (this._totalCount + totalCount) : data.totalCount;
        const items = data.items || [];
        if (items.length) {
            const maxGroupLengths = items.reduce((lengths, item) => {
                lengths[item.groupId] = item.genes.reduce((max, gene) => Math.max(max, gene.protLen), 0);
                return lengths;
            }, {});
            const getHomologData = (item) => {
                return (item.genes || []).map((g, i) => ({
                    target: this.genesOfInterest[0].chip,
                    species: g.speciesScientificName,
                    'homology type': capitalize('HOMOLOG'),
                    homologue: g.symbol,
                    geneId: g.geneId,
                    'homology group': item.groupId,
                    'protein': g.title,
                    aa: g.protLen,
                    domains: {
                        domains: (g.domains || []).map(d => ({
                            id: d.pssmId,
                            start: d.begin,
                            end: d.end,
                            name: d.cddName,
                            color: calculateColor(d.cddName)
                        })),
                        homologLength: g.protLen,
                        maxHomologLength: maxGroupLengths[item.groupId],
                        accession_id: g.accession_id
                    }
                }));
            };
            const homologsData = items.reduce((acc, item) => (
                [...acc, ...getHomologData(item)]
            ), []);
            this._homologsData = [
                ...(this._homologsData || []),
                ...homologsData
            ];
        }
    }

    async getData() {
        this.loadingData = true;
        const results = [];
        results.push(await this.getAllHomologs());
        results.push(await this.getAllHomologenes());
        this.setTotalPages();
        this.loadingData = false;
        return results.some(v => v);
    }

    async getAllHomologs() {
        return Promise.all(this.allGenes.map(async (gene) => (
            await this.getHomologs(gene.geneId)
        )))
            .then(values => values.some(v => v));
    }

    async getAllHomologenes() {
        return Promise.all(this.allGenes.map(async (gene) => (
            await this.getHomologene(gene.geneName)
        )))
            .then(values => values.some(v => v));
    }

    setTotalPages() {
        this._totalPages = Math.ceil(this._homologsData.length / this.pageSize);
        this._emptyResults = !this._homologsData.length;
    }

    async getHomologs(id) {
        if (!id) {
            return new Promise.resolve(true);
        }
        const request = {
            geneId: id,
            page: 1,
            pageSize: 10
        }
        return new Promise(resolve => {
            this.genomeDataService.getOrthoParaLoad(request)
                .then(data => {
                    this.setHomologsData(data);
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = this._errorMessageList
                        ? this._errorMessageList.push(err.message)
                        : [err.message];
                    resolve(false);
                });
        });
    }

    async getHomologene(name) {
        if (!name) {
            return Promise.resolve(true);
        }
        const request = {
            query: name,
            page: 1,
            pageSize: 10
        }
        return new Promise(resolve => {
            this.genomeDataService.getHomologeneLoad(request)
                .then(data => {
                    this.setHomologeneData(data);
                    resolve(data);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = this._errorMessageList
                        ? this._errorMessageList.push(err.message)
                        : [err.message];
                    resolve(false);
                });
        });
    }

    resetData() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._alignment = null;
        this._homologsData = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._emptyResults = false;
    }
}
