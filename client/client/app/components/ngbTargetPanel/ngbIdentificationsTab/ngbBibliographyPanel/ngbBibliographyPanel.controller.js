const LLM_MODELS = [
    {
        name: 'ChatGPT 3.5',
        value: 'OPENAI_GPT_35'
    }, {
        name: 'ChatGPT 4.0',
        value: 'OPENAI_GPT_40'
    }, {
        name: 'Google PaLM2',
        value: 'GOOGLE_PALM_2'
    }
];

export default class ngbBibliographyPanelController {

    llmModelValue = this.llmModels[0].value;

    get llmModels() {
        return LLM_MODELS;
    }

    static get UID() {
        return 'ngbBibliographyPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbBibliographyPanelService) {
        Object.assign(this, {$scope, $timeout, ngbBibliographyPanelService});
        const refresh = this.refresh.bind(this);
        dispatcher.on('target:identification:publications:page:changed', refresh);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:drugs:source:changed', refresh);
        });
    }

    get publications() {
        return this.ngbBibliographyPanelService.publications;
    }

    refresh() {
        this.$timeout(() => this.$scope.$apply());
    }

    get loadingPublications() {
        return this.ngbBibliographyPanelService.loadingPublications;
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

    get totalPages() {
        return this.ngbBibliographyPanelService.totalPages;
    }
    get currentPage() {
        return this.ngbBibliographyPanelService.currentPage;
    }
    set currentPage(value) {
        this.ngbBibliographyPanelService.currentPage = value;
    }

    $onInit() {
        (this.refresh)();
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
        this.$timeout(() => this.$scope.$apply());
    }

    onClickSelect(event) {
        event.preventDefault();
        event.stopPropagation();
    }

    onChangeLlmModel() {
        this.generateSummary();
    }
}
