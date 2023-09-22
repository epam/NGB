import buildMainInfoBlocks from './utilities/build-main-info-blocks';

export default class ngbDiseasesTabController {

    descriptionCollapsed = true;
    _mainInfoBlocks = [];
    title;
    description;
    synonyms;

    get mainInfo () {
        return this._mainInfoBlocks;
    }

    static get UID() {
        return 'ngbDiseasesTabController';
    }

    constructor($scope, $timeout, ngbDiseasesTabService) {
        Object.assign(this, {$scope, $timeout, ngbDiseasesTabService});
        if (this.diseasesData) {
            this.refreshData();
        }
    }

    refreshData() {
        this.setDiseasesData();
        this.$timeout(() => this.$scope.$apply());
    }

    get diseaseModel() {
        return this.ngbDiseasesTabService.diseaseModel;
    }
    set diseaseModel(value) {
        this.ngbDiseasesTabService.diseaseModel = value;
    }
    get searchText() {
        return this.ngbDiseasesTabService.searchText;
    }
    set searchText(value) {
        this.ngbDiseasesTabService.searchText = value;
    }
    get diseasesList() {
        return this.ngbDiseasesTabService.diseasesList;
    }
    get loadingData() {
        return this.ngbDiseasesTabService.loadingData;
    }
    set loadingData(value) {
        this.ngbDiseasesTabService.loadingData = value;
    }
    get failedResult () {
        return this.ngbDiseasesTabService.failedResult;
    }
    get errorMessageList () {
        return this.ngbDiseasesTabService.errorMessageList;
    }
    get diseasesData () {
        return this.ngbDiseasesTabService.diseasesData;
    }

    async getDiseasesList() {
        return new Promise(resolve => {
            this.ngbDiseasesTabService.getDisease(this.searchText)
                .then(data => resolve(data))
                .catch(err => resolve([]))
        });
    }

    diseaseChanged(disease) {
        this.diseaseModel = disease;
        this.searchText = disease.name;
    }

    onBlur () {
        if (this.searchText && this.diseasesList.length) {
            if (this.searchText !== this.diseaseModel.name) {
                this.diseaseChanged(this.diseasesList[0]);
            } else {
                this.diseaseChanged(this.diseaseModel);
            }
        } else {
            this.diseaseModel = {};
        }
    }

    onKeyPress (event) {
        switch ((event.code || '').toLowerCase()) {
            case 'enter':
                this.onBlur();
                break;
            default:
                break;
        }
    }

    async searchDisease() {
        this.loadingData = true;
        await this.ngbDiseasesTabService.getDiseaseData(this.diseaseModel.id);
        this.setDiseasesData();
        this.$timeout(() => this.$scope.$apply());
    }

    setDiseasesData() {
        const {name, description, synonyms} = this.diseasesData;
        this.title = name;
        this.description = description;
        this.synonyms = synonyms;
        this.refreshInfoBlocks();
    }

    refreshInfoBlocks() {
        this._mainInfoBlocks = buildMainInfoBlocks(this.diseasesData);
    }

    toggleDescriptionCollapsed () {
        this.descriptionCollapsed = !this.descriptionCollapsed;
    }
}
