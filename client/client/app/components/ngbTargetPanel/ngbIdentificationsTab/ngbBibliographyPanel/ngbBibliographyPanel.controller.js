export default class ngbBibliographyPanelController {

    _publications = null;

    get publications() {
        return this._publications;
    }

    static get UID() {
        return 'ngbBibliographyPanelController';
    }

    constructor($scope, $timeout, ngbBibliographyPanelService) {
        Object.assign(this, {$scope, $timeout, ngbBibliographyPanelService});
    }

    get loadingData() {
        return this.ngbBibliographyPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbBibliographyPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbBibliographyPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbBibliographyPanelService.errorMessageList;
    }
    get emptyResults() {
        return this.ngbBibliographyPanelService.emptyResults;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        await this.getPublications();
    }

    async getPublications () {
        this.loadingData = true;
        this._publications = await this.ngbBibliographyPanelService.getPublicationsResults()
            .then(success => {
                if (success) {
                    return this.ngbBibliographyPanelService.publicationsResults;
                }
                return [];
            });
        this.$timeout(::this.$scope.$apply);
        console.log(this._publications);
    }
}
