import {calculateColor} from '../../../../shared/utils/calculateColor';

const PAGE_SIZE = 10;

const EXPORT_SOURCE = 'HOMOLOGY';

const capitalize = (string) => {
    if (string) {
        return string.slice(0, 1).toUpperCase().concat(string.slice(1).toLowerCase());
    }
    return '';
};

export default class ngbGenomicsPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }
    get exportSource() {
        return EXPORT_SOURCE;
    }

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _alignment = null;
    _genomicsData = null;
    _genomicsResults = null;
    _currentPage = 1;
    _totalPages = 0;
    _emptyResults = false;
    _filterInfo = null;
    fieldList = {};

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
    get genomicsData() {
        return this._genomicsData;
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
    get filterInfo() {
        return this._filterInfo;
    }
    set filterInfo(value) {
        this._filterInfo = value;
    }
    get defaultFilter() {
        return {
            target: this.interestGenes.map(g => g.chip),
            species: this.translationalSpecies
        };
    }
    get tableResults() {
        return this._genomicsResults && this._genomicsResults.length;
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        return new ngbGenomicsPanelService(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService, genomeDataService) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService, genomeDataService});
        dispatcher.on('target:identification:changed', this.resetData.bind(this));
    }

    get translationalSpecies() {
        const { translational = [] } = this.ngbTargetPanelService.identificationTarget || {};
        return translational.map(s => ({
            taxId: s.taxId,
            name: s.speciesName
        }));
    }

    get interestGenes() {
        const { interest = [] } = this.ngbTargetPanelService.identificationTarget || {};
        return [...interest];
    }

    get interestTaxIds() {
        return this.interestGenes.map(g => g.taxId);
    }

    get geneIdsOfInterest() {
        return this.ngbTargetPanelService.geneIdsOfInterest;
    }

    get translationalGeneIds() {
        return this.ngbTargetPanelService.translationalGeneIds;
    }

    getChipByGeneId(id) {
        return this.ngbTargetPanelService.getChipByGeneId(id);
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
            targetLength: target.baseString.length - 1,
            queryName: getName(query.name),
            queryTooltip: query.name,
            querySequence: query.baseString,
            queryStart: 1,
            queryEnd: query.baseString.length,
            queryLength: query.baseString.length - 1
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

    setGenomicsResults () {
        let data = [...this._genomicsData];
        if (!data || !data.length) {
            this._genomicsResults = [];
            this.setTotalPages();
            return;
        }
        if (this._filterInfo) {
            Object.entries(this._filterInfo).map(([key, value]) => {
                const isInclude = (item) => {
                    if (key === 'species') {
                        return value.some(v => item[key].taxId === v.taxId);
                    }
                    return value.some(v => item[key].toLowerCase().includes(v.toLowerCase()));
                };
                data = data.filter(item => {
                    return (
                        value.includes(item[key]) ||
                        isInclude(item)
                    );
                })
            })
        }
        this._genomicsResults = data;
        this.setTotalPages();
    }

    getGenomicsResults() {
        const start = (this.currentPage - 1) * this.pageSize;
        const end = this.currentPage * this.pageSize;
        return this._genomicsResults.slice(start, end);
    }

    setHomologsData(data) {
        Object.entries(data).map(([id, value]) => {
            const items = value || [];
            if (items.length) {
                const maxGroupLengths = items.reduce((lengths, item) => {
                    lengths[item.groupId] = item.homologs.reduce(
                        (max, homolog) => Math.max(max, homolog.protLen),
                        0);
                    return lengths;
                }, {});
                const getHomologData = (item) => {
                    return (item.homologs || [])
                        .filter(h => !this.interestTaxIds.includes(h.taxId))
                        .map(h => ({
                            target: this.getChipByGeneId(id),
                            species: {
                                taxId: h.taxId,
                                name: h.speciesScientificName
                            },
                            'homology type': capitalize(item.type),
                            homologue: h.symbol || `id: ${h.geneId}`,
                            geneId: h.geneId,
                            'homology group': item.proteinName,
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
                ), [])
                this._genomicsData = [
                    ...(this._genomicsData || []),
                    ...homologsData
                ];
            }
        });
    }

    setHomologeneData(data) {
        Object.entries(data).map(([id, value]) => {
            const items = value || [];
            if (items.length) {
                const maxGroupLengths = items.reduce((lengths, item) => {
                    lengths[item.groupId] = item.genes.reduce(
                        (max, gene) => Math.max(max, gene.protLen),
                        0);
                    return lengths;
                }, {});
                const getHomologData = (item) => {
                    return (item.genes || [])
                        .filter(g => !this.interestTaxIds.includes(g.taxId))
                        .map(g => ({
                            target: this.getChipByGeneId(id),
                            species: {
                                taxId: g.taxId,
                                name: g.speciesScientificName
                            },
                            'homology type': capitalize('HOMOLOG'),
                            homologue: g.symbol,
                            geneId: g.geneId,
                            'homology group': item.caption,
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
                this._genomicsData = [
                    ...(this._genomicsData || []),
                    ...homologsData
                ];
            }
        });
    }

    async getGenomicsData() {
        this.loadingData = true;
        const ids = this.interestGenes.map(g => g.geneId);
        const results = await Promise.allSettled([this.getHomologs(ids), this.getHomologene(ids)]);
        this.setGenomicsResults();
        this.loadingData = false;
        this.setFieldList();
        return results.some(v => v);
    }

    setTotalPages() {
        this._totalPages = Math.ceil(this._genomicsResults.length / this.pageSize);
        this._emptyResults = !this._genomicsResults.length;
    }

    async getHomologs(ids) {
        if (!ids || !ids.length) {
            return new Promise.resolve(true);
        }
        return new Promise(resolve => {
            this.targetDataService.getOrthoParaLoad({geneIds: ids})
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

    async getHomologene(ids) {
        if (!ids || !ids.length) {
            return Promise.resolve(true);
        }
        return new Promise(resolve => {
            this.targetDataService.getHomologeneLoad({geneIds: ids})
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

    setFieldList() {
        this.fieldList = {
            target: this.interestGenes.map(g => g.chip),
            species: this.translationalSpecies,
            'homology type': Array.from(new Set(this._genomicsResults.map(g => g['homology type'])))
        };
        this.dispatcher.emitSimpleEvent('genomics:filters:list');
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value && value.length) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
    }

    resetData() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._alignment = null;
        this._genomicsData = null;
        this._genomicsResults = null;
        this._currentPage = 1;
        this._totalPages = 0;
        this._emptyResults = false;
        this._filterInfo = null;
    }

    exportResults() {
        const source = this.exportSource;
        if (!this.geneIdsOfInterest || !this.translationalGeneIds) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        return this.targetDataService.getTargetExport(this.geneIdsOfInterest, this.translationalGeneIds, source);
    }
}
