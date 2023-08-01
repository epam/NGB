const EMPTY_SEQUENCES_RESULT = [
    {
        name: 'GENOMIC'
    }, {
        name: 'mRNA'
    }, {
        name: 'PROTEINS'
    }
];

export default class ngbSequencesPanelService {

    _seqienceResults = EMPTY_SEQUENCES_RESULT;
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;

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
    get seqienceResults() {
        return this._seqienceResults;
    }

    static instance (ngbTargetPanelService, targetDataService, projectContext) {
        return new ngbSequencesPanelService(ngbTargetPanelService, targetDataService, projectContext);
    }

    constructor(ngbTargetPanelService, targetDataService, projectContext) {
        Object.assign(this, {ngbTargetPanelService, targetDataService, projectContext});
    }

    get geneIds() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.geneId)];
    }

    setSeqienceResults(result) {
        const {reference, mrnas, proteins} = result[0];
        const [GENOMIC, mRNA, PROTEINS] = this._seqienceResults;
        if (reference) {
            GENOMIC.value = [{
                name: 'reference',
                ...reference
            }];
        }
        if (mrnas) {
            mRNA.value = [...mrnas];
        }
        if (proteins) {
            PROTEINS.value = [...proteins];
        }
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
                    this.setSeqienceResults(data);
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
