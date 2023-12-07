export default class ngbDiseasesTabService {

    _searchText = '';
    _diseaseModel = {};
    diseasesList = [];
    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    _diseasesData = null;
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
    get diseasesData() {
        return this._diseasesData;
    }
    set diseasesData(value) {
        this._diseasesData = value;
    }

    static instance ($timeout, dispatcher, targetDataService, targetContext) {
        return new ngbDiseasesTabService($timeout, dispatcher, targetDataService, targetContext);
    }

    constructor($timeout, dispatcher, targetDataService, targetContext) {
        Object.assign(this, {$timeout, dispatcher, targetDataService, targetContext});
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

    async searchDisease(disease) {
        const {id} = disease;
        if (this._diseasesData && id === this._diseasesData.id) return;
        this._loadingData = true;
        await this.getDiseaseData(id);
        await this.getDiseaseTotalCounts(id);
        this.targetContext.setCurrentDisease(disease);
        this.$timeout(() => this.dispatcher.emit('target:diseases:disease:changed'));
    }

    getDiseaseData(id) {
        return new Promise(resolve => {
            this.targetDataService.getDiseaseData(id)
                .then(data => {
                    this._failedResult = false;
                    this._errorMessageList = null;
                    this._diseasesData = data;
                    this._loadingData = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedResult = true;
                    this._errorMessageList = [err.message];
                    this._diseasesData = null;
                    this._loadingData = false;
                    resolve(false);
                });
        });
    }

    getDiseaseTotalCounts(id) {
        return new Promise(resolve => {
            this.targetDataService.getDiseaseIdentification(id)
                .then(data => {
                    this.setTotalCounts(data);
                    resolve(true);
                })
                .catch(err => {
                    this.setTotalCounts(null);
                    resolve(false);
                });
        });
    }

    setTotalCounts(total) {
        if (this._diseasesData) {
            const {targetsCount, knownDrugsCount, knownDrugsRecordsCount} = total || {};
            this._diseasesData.targetsCount = targetsCount || 0;
            this._diseasesData.knownDrugsCount = knownDrugsCount || 0;
            this._diseasesData.knownDrugsRecordsCount = knownDrugsRecordsCount || 0;
        }
    }

    async viewDiseaseFromTable(disease) {
        this.resetData();
        this._loadingData = true;
        const {id, name} = disease;
        this._searchText = name;
        this._diseaseModel = {
            id: disease.id,
            name: disease
        };
        await this.searchDisease(disease);
    }

    resetData() {
        this._searchText = '';
        this._diseaseModel = {};
        this.diseasesList = [];
        this._loadingData = false;
        this._failedResult = false;
        this._errorMessageList = null;
        this._diseasesData = null;
    }
}
