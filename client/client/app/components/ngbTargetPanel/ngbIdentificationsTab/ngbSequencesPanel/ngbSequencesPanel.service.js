const PAGE_SIZE = 10;

const EXPORT_SOURCE = 'SEQUENCES';
const PARASITE = 'PARASITE';

export default class ngbSequencesPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }

    get exportSource() {
        return EXPORT_SOURCE;
    }
    get parasiteType() {
        return PARASITE;
    }

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _emptyResults = false;
    _genes = [];
    _selectedGeneId;
    _allSequences = null;
    _sequencesResults = null;
    _sequencesReference = null;
    _totalPages = 0;

    _includeLocal = false;
    _includeAdditionalGenes = false;

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = !!value;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }
    get emptyResults() {
        return this._emptyResults;
    }
    get genes() {
        return this._genes;
    }
    get selectedGeneId() {
        return this._selectedGeneId;
    }
    set selectedGeneId(id) {
        this._selectedGeneId = id;
    }
    get allSequences() {
        return this._allSequences;
    }
    get sequencesResults() {
        return this._sequencesResults;
    }
    get sequencesReference() {
        return this._sequencesReference;
    }
    get totalPages() {
        return this._totalPages;
    }

    get selectedGene() {
        return this.genes.filter(gene => gene.geneId === this.selectedGeneId)[0];
    }

    get includeLocal() {
        return this._includeLocal;
    }
    set includeLocal(value) {
        this._includeLocal = value;
    }

    get includeAdditionalGenes() {
        return this._includeAdditionalGenes;
    }
    set includeAdditionalGenes(value) {
        this._includeAdditionalGenes = value;
    }

    static instance ($timeout, dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbSequencesPanelService($timeout, dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor($timeout, dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {$timeout, dispatcher, ngbTargetPanelService, targetDataService});
        this.updateGenes();
        this.updateSettings();
        dispatcher.on('target:identification:changed', this.targetChanged.bind(this));
    }

    get genesIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    get geneIdsOfInterest() {
        return this.ngbTargetPanelService.geneIdsOfInterest;
    }

    get translationalGeneIds() {
        return this.ngbTargetPanelService.translationalGeneIds;
    }

    get targetId () {
        const {target} = this.ngbTargetPanelService.identificationTarget;
        if (target && target.id) {
            return target.id;
        }
        return undefined;
    }

    get isParasite() {
        const {target} = (this.ngbTargetPanelService.identificationTarget || {});
        return target && target.type === this.parasiteType;
    }

    async targetChanged() {
        this.resetSequencesData();
        this.updateGenes();
        this.updateSettings();
    }

    updateGenes() {
        this._genes = this.ngbTargetPanelService.allGenes.slice();
        const gene = this._genes[0];
        this.selectedGeneId = gene ? gene.geneId : undefined;
    }

    updateSettings() {
        this._includeLocal = this.isParasite;
        this._includeAdditionalGenes = this.isParasite;
    }

    getTarget(id) {
        if (!id) return;
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    setEmtyResults() {
        this._sequencesResults = [];
        this._emptyResults = true;
        this._totalPages = 0;
    }

    setSequencesResults(sequences) {
        if (!sequences) {
            this.setEmtyResults();
            return;
        }
        const target = this.getTarget(this.selectedGeneId);
        const results = sequences.map(sequence => {
            const {mrna = {}, protein = {}} = sequence;
            return {
                target,
                'transcript': {
                    id: mrna.id || mrna.name,
                    url: mrna.url
                },
                'mrna length': mrna.length,
                'protein': {
                    id: protein.id,
                    url: protein.url
                },
                'protein length': protein.length,
                'protein name': protein.name
            }
        });
        this._sequencesResults = results;
        this._emptyResults = !results.length;
        this._totalPages = Math.ceil(results.length / this.pageSize);
    }

    setSequencesReference (reference) {
        if (!reference) {
            this._sequencesReference = null;
            return;
        }
        const referencesObject = reference.reduce((acc, r) => {
            r.name = r.name || 'reference';
            acc[r.id] = acc[r.id] || r;
            return acc;
        }, {});
        this._sequencesReference = Object.entries(referencesObject)
            .map(([key, value]) => ({
                id: key,
                ...value
            }));
    }

    setAllSequences(result) {
        this._allSequences = this.genesIds.reduce((acc, id) => {
            const data = result.filter(item => item.geneId.toLowerCase() === id.toLowerCase()) || {};
            acc[id.toLowerCase()] = {
                reference: data.map(i => i.reference).filter(i => i) || null,
                sequences: data.reduce((acc, i) => {
                    if (i.sequences) {
                        acc = [...acc, ...i.sequences];
                    }
                    return acc;
                }, []) || null
            };
            return acc;
        }, {});
        this.dispatcher.emit('target:identification:sequences:updated', result);
        this.setSequenceData();
    }

    setSequenceData () {
        const data = this.allSequences[this.selectedGeneId.toLowerCase()];
        if (!data) {
            this._sequencesReference = null;
            this.setEmtyResults();
            return;
        }
        this.setSequencesReference(data.reference)
        this.setSequencesResults(data.sequences);
    }

    getSequencesData() {
        if (!this.genesIds || !this.genesIds.length) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        const params = {
            includeLocal: this.includeLocal,
            includeAdditionalGenes: this.includeAdditionalGenes,
            targetId: this.targetId
        };
        return new Promise(resolve => {
            this.targetDataService.getSequencesTableResults(this.genesIds, params)
                .then((data) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.setAllSequences(data);
                    this.loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._allSequences = null;
                    this._sequencesReference = null;
                    this._sequencesResults = null;
                    this._emptyResults = false;
                    this._totalPages = 0;
                    this.loadingData = false;
                    resolve(false);
                });
        });
    }

    resetSequenceResults() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._emptyResults = false;
        this._sequencesResults = null;
        this._totalPages = 0;
    }

    async resetSequencesData() {
        this._genes = [];
        this._selectedGeneId = undefined;
        this._allSequences = null;
        this._includeLocal = false;
        this._includeAdditionalGenes = false;
        this.resetSequenceResults();
    }

    resetAllSequences() {
        this._allSequences = null;
    }

    exportResults() {
        const source = this.exportSource;
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => {
                resolve(true);
            });
        }
        if (!this.targetId) {
            return this.targetDataService.getTargetExportGeneId(this.geneIdsOfInterest[0], source);
        }
        return this.targetDataService.getTargetExport(
            this.geneIdsOfInterest,
            this.translationalGeneIds,
            source,
            this.targetId
        );
    }
}
