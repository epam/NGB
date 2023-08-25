export default class ngbGenomicsPanelService {

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _alignment = null;

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

    static instance (targetDataService) {
        return new ngbGenomicsPanelService(targetDataService);
    }

    constructor(targetDataService) {
        Object.assign(this, {targetDataService});
    }

    setAlignment(data) {
        this._alignment = data.map(item => ({
            name: item.name,
            bases: item.bases
        }));
    }

    getTargetAlignment(targetId, sequenceIds) {
        return new Promise(resolve => {
            this.targetDataService.getTargetAlignment(targetId, sequenceIds)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.setAlignment(data);
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._alignments = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }
}
