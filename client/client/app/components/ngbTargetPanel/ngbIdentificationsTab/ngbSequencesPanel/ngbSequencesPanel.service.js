const EMPTY_SEQUENCES_DATA = {
    reference: [],
    mrnas: [],
    proteins: []
};

export default class ngbSequencesPanelService {

    get emptySequencesData() {
        return EMPTY_SEQUENCES_DATA;
    }

    _sequenceData = null;
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _sequenceResults = null;
    _genes = [];
    _selectedGeneId;

    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }

    get failedResult() {
        return this._failedResult;
    }
    set failedResult(value) {
        this._failedResult = value;
    }

    get errorMessageList() {
        return this._errorMessageList;
    }
    set errorMessageList(value) {
        this._errorMessageList = value;
    }

    get sequenceData() {
        return this._sequenceData;
    }
    set sequenceData(value) {
        this._sequenceData = Object.entries(value)
            .map(([name, value]) => ({ name, value }));
    }

    get sequenceResults() {
        return this._sequenceResults;
    }
    get genes() {
        return this._genes;
    }

    get selectedGeneId() {
        return this._selectedGeneId;
    }
    set selectedGeneId(selectedGeneId) {
        if (
            this._selectedGeneId !== !!selectedGeneId) {
            this._selectedGeneId = selectedGeneId;
            this.dispatcher.emit('target:identification:sequence:gene:changed');
        }
    }

    static instance (dispatcher, ngbTargetPanelService, targetDataService, projectContext) {
        return new ngbSequencesPanelService(dispatcher, ngbTargetPanelService, targetDataService, projectContext);
    }

    constructor(dispatcher, ngbTargetPanelService, targetDataService, projectContext) {
        Object.assign(this, {dispatcher, ngbTargetPanelService, targetDataService, projectContext});
        dispatcher.on('target:identification:changed', this.updateGenes.bind(this));
        this.setEmptySequenceData();
        this.updateGenes();
    }

    updateGenes() {
        this._genes = this.ngbTargetPanelService.allGenes.slice();
        const gene = this._genes[0];
        this.selectedGeneId = gene ? gene.geneId : undefined;
    }

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    setEmptySequenceData() {
        this.sequenceData = {...this.emptySequencesData};
    }

    setSequenceData() {
        this.setEmptySequenceData();
        const result = this.sequenceResults
            .filter(item => item.geneId.toLowerCase() === this.selectedGeneId.toLowerCase())[0];
        if (!result) return;
        const {reference, mrnas, proteins} = result;
        const data = {...this.emptySequencesData};
        if (reference) {
            data.reference = [{
                name: 'reference',
                ...reference
            }];
        }
        if (mrnas) {
            data.mrnas = [...mrnas];
        }
        if (proteins) {
            data.proteins = [...proteins];
        }
        this.sequenceData = data;
    }

    getSequencesResults() {
        if (!this.geneIds || !this.geneIds.length) {
            return new Promise(resolve => {
                this.loadingData = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getSequencesResults(this.geneIds)
                .then((data) => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._sequenceResults = data;
                    this.setSequenceData();
                    this.loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this.loadingData = false;
                    resolve(false);
                });
        });
    }
}
