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

    constructor($scope, $timeout, dispatcher, ngbDiseasesTabService, ngbDiseasesTargetsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbDiseasesTabService, ngbDiseasesTargetsPanelService});
        if (this.diseasesData) {
            this.refreshData();
        }
        const setDiseasesData = this.setDiseasesData.bind(this);
        dispatcher.on('target:diseases:disease:changed', setDiseasesData);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:disease:changed', setDiseasesData);
        });
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
    get openedPanels() {
        return this.ngbDiseasesTabService.openedPanels;
    }

    async getDiseasesList() {
        return new Promise(resolve => {
            this.ngbDiseasesTabService.getDisease(this.searchText)
                .then(data => resolve(data))
                .catch(err => resolve([]))
        });
    }

    diseaseChanged(disease) {
        if (!disease) return;
        this.diseaseModel = disease;
        this.searchText = disease.name;
        this.searchDisease();
    }

    onBlur () {
        if (this.searchText && this.diseasesList.length) {
            if (this.searchText !== this.diseaseModel.name) {
                this.diseaseModel = this.diseasesList[0];
                this.searchText = this.diseasesList[0].name;
            } else {
                this.searchText = this.diseaseModel.name;
            }
        } else {
            this.diseaseModel = {};
        }
        this.searchDisease();
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
        if (!this.diseaseModel) return;
        await this.ngbDiseasesTabService.searchDisease(this.diseaseModel.id);
    }

    setDiseasesData() {
        const {name, description, synonyms} = this.diseasesData || {};
        this.title = name;
        this.description = description;
        this.synonyms = (synonyms || []).filter(s => s);
        this.refreshInfoBlocks();
        this.$timeout(() => this.$scope.$apply());
    }

    refreshInfoBlocks() {
        this._mainInfoBlocks = buildMainInfoBlocks(this.diseasesData);
    }

    toggleDescriptionCollapsed () {
        this.descriptionCollapsed = !this.descriptionCollapsed;
    }
}
