const LLM_MODELS = [
    {
        name: 'ChatGPT 3.5',
        value: 'OPENAI_GPT_35'
    }, {
        name: 'ChatGPT 4.0',
        value: 'OPENAI_GPT_40'
    }
];

export default class ngbBibliographyPanelController {

    _publications = null;
    llmModelValue = this.llmModels[0].value;

    get publications() {
        return this._publications;
    }

    get llmModels() {
        return LLM_MODELS;
    }

    static get UID() {
        return 'ngbBibliographyPanelController';
    }

    constructor($scope, $timeout, ngbBibliographyPanelService) {
        Object.assign(this, {$scope, $timeout, ngbBibliographyPanelService});
    }

    get loadingPublications() {
        return this.ngbBibliographyPanelService.loadingPublications;
    }
    set loadingPublications(value) {
        this.ngbBibliographyPanelService.loadingPublications = value;
    }
    get failedPublications() {
        return this.ngbBibliographyPanelService.failedPublications;
    }
    get publicationsError() {
        return this.ngbBibliographyPanelService.publicationsError;
    }
    get emptyPublications() {
        return this.ngbBibliographyPanelService.emptyPublications;
    }

    get loadingSummary() {
        return this.ngbBibliographyPanelService.loadingSummary;
    }
    set loadingSummary(value) {
        this.ngbBibliographyPanelService.loadingSummary = value;
    }
    get failedSummary() {
        return this.ngbBibliographyPanelService.failedSummary;
    }
    get summaryError() {
        return this.ngbBibliographyPanelService.summaryError;
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        await this.getPublications();
    }

    async getPublications () {
        this.loadingPublications = true;
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

    async generateSummary(event) {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        this.loadingSummary = true;
        this.summary = await this.ngbBibliographyPanelService.getLlmSummary(this.llmModelValue)
            .then(success => {
                if (success) {
                    return this.ngbBibliographyPanelService.summaryResult;
                }
                return null;
            });
        this.$timeout(::this.$scope.$apply);
    }

    onClickSelect(event) {
        event.preventDefault();
        event.stopPropagation();
    }

    onChangeLlmModel() {
        this.generateSummary();
    }
}