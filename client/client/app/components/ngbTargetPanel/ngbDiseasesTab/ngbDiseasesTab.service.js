export default class ngbDiseasesTabService {

    _searchText = '';
    _diseaseModel = {};
    diseasesList = [];
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    diseasesData = null;
    _openedPanels = {
        drugs: false,
        targets: false,
    }

    get diseaseModel() {
        return this._diseaseModel;
    }
    set diseaseModel(value) {
        this._diseaseModel = value;
    }
    get searchText() {
        return this._searchText;
    }
    set searchText(value) {
        this._searchText = value;
    }
    get loadingData() {
        return this._loadingData;
    }
    set loadingData(value) {
        this._loadingData = value;
    }
    get failedResult () {
        return this._failedResult;
    }
    get errorMessageList () {
        return this._errorMessageList;
    }

    get openedPanels() {
        return this._openedPanels;
    }
    set openedPanels(value) {
        this._openedPanels = value;
    }

    static instance (targetDataService) {
        return new ngbDiseasesTabService(targetDataService);
    }

    constructor(targetDataService) {
        Object.assign(this, {targetDataService});
    }

    setDiseasesList(list) {
        if (!list) {
            this.diseasesList = [];
        } else {
            this.diseasesList = Object.entries(list).map(([id, name]) => ({id, name}));
        }
    }

    getDisease(text) {
        if (!text) {
            return new Promise(resolve => {
                this.diseasesList = [];
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getDisease(text)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.setDiseasesList(data);
                    resolve(this.diseasesList);
                })
                .catch(err => {
                    this.diseasesList = [];
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    resolve(this.diseasesList);
                });
        });
    }

    getDiseaseData(id) {
        return new Promise(resolve => {
            this.targetDataService.getDiseaseData(id)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this.diseasesData = data;
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this.diseasesData = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    closeAll() {
        this._openedPanels = {
            drugs: false,
            targets: false,
        };
    }
}
