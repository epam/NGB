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

    static instance ($timeout, dispatcher, targetDataService) {
        return new ngbDiseasesTabService($timeout, dispatcher, targetDataService);
    }

    constructor($timeout, dispatcher, targetDataService) {
        Object.assign(this, {$timeout, dispatcher, targetDataService});
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

    async viewDiseaseFromTable(disease) {
        this.resetData();
        this._loadingData = true;
        const {id, name} = disease;
        this._searchText = name;
        this._diseaseModel = disease;
        await this.getDiseaseData(id);
        this.$timeout(() => this.dispatcher.emit('target:diseases:details:finished'));
    }

    closeAll() {
        this._openedPanels = {
            drugs: false,
            targets: false,
        };
    }

    resetData() {
        this._searchText = '';
        this._diseaseModel = {};
        this.diseasesList = [];
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this.diseasesData = null;
    }
}
