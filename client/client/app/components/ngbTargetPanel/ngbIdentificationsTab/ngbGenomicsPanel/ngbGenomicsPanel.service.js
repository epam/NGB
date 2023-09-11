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

    static instance (dispatcher, targetDataService) {
        return new ngbGenomicsPanelService(dispatcher, targetDataService);
    }

    constructor(dispatcher, targetDataService) {
        Object.assign(this, {targetDataService});
        dispatcher.on('target:identification:changed', this.resetData.bind(this));
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

    resetData() {
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._alignment = null;
    }
}
