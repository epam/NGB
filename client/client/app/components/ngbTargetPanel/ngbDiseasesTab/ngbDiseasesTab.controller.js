export default class ngbDiseasesTabController {

    searchText;
    diseaseModel;
    diseasesList = [];

    static get UID() {
        return 'ngbDiseasesTabController';
    }

    constructor() {
        Object.assign(this, {});
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

    searchDisease() {}

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
