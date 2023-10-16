const PAGE_SIZE = 10;

const EXPORT_SOURCE = 'SEQUENCES';

export default class ngbSequencesPanelService {

    get pageSize() {
        return PAGE_SIZE;
    }

    get exportSource() {
        return EXPORT_SOURCE;
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

    static instance ($timeout, dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbSequencesPanelService($timeout, dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor($timeout, dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {$timeout, dispatcher, ngbTargetPanelService, targetDataService});
        this.updateGenes();
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

    async targetChanged() {
        this.resetSequencesData();
        this.updateGenes();
    }

    updateGenes() {
        this._genes = this.ngbTargetPanelService.allGenes.slice();
        const gene = this._genes[0];
        this.selectedGeneId = gene ? gene.geneId : undefined;
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
                    id: mrna.id,
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
        reference.name = reference.name || 'reference';
        this._sequencesReference = reference;
    }

    setAllSequences(result) {
        this._allSequences = this.genesIds.reduce((acc, id) => {
            const data = result.filter(item => item.geneId.toLowerCase() === id.toLowerCase())[0] || {};
            acc[id.toLowerCase()] = {
                reference: data.reference || null,
                sequences: data.sequences || null
            };
            return acc;
        }, {});
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
        return new Promise(resolve => {
            this.targetDataService.getSequencesTableResults(this.genesIds)
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
        this.resetSequenceResults();
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
