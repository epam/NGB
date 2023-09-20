import buildMainInfoBlocks from './utilities/build-main-info-blocks';

const DISEASES_LIST = ['Lung Carcinoma', 'Non-small cell lung carcinoma', 'Lung Cancer'];
export default class ngbDiseasesTabController {

    searchText;
    diseaseModel;
    descriptionCollapsed = true;
    _mainInfoBlocks = [];

    get diseasesList() {
        return DISEASES_LIST;
    }

    get mainInfo () {
        return this._mainInfoBlocks;
    }

    static get UID() {
        return 'ngbDiseasesTabController';
    }

    constructor(ngbDiseasesTabService) {
        Object.assign(this, {ngbDiseasesTabService});
    }

    get diseasesData () {
        return this.ngbDiseasesTabService.diseasesData;
    }

    getFilteredDiseases() {
        return this.diseasesList.filter(disease => {
            return (new RegExp(`${this.searchText}`, 'i')).test(disease);
        });
    }

    diseaseChanged(disease) {
        this.diseaseModel = disease;
        this.searchText = this.diseaseModel;
    }

    searchDisease() {
        this.diseasesResults = true;
        this.title = this.diseaseModel;
        this.synonyms = ['malignant neoplasm of lung', 'cancer of lung', 'malignant lung neoplasm'];
        this.description = 'a malignant neoplasm involving the lung.';
        this.refreshInfoBlocks();
    }

    refreshInfoBlocks() {
        this._mainInfoBlocks = buildMainInfoBlocks(this.diseasesData);
    }

    toggleDescriptionCollapsed () {
        this.descriptionCollapsed = !this.descriptionCollapsed;
    }

    onBlur () {
        if (this.searchText) {
            this.diseaseChanged(this.getFilteredDiseases()[0])
        } else {
            this.diseaseModel = undefined;
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
}
