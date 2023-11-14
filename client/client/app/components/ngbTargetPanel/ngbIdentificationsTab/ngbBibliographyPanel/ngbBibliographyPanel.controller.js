export default class ngbBibliographyPanelController {

    searchText = '';

    static get UID() {
        return 'ngbBibliographyPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbBibliographyPanelService,
        targetLLMService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            ngbBibliographyPanelService,
            targetLLMService
        });
        this._summaryWasGenerated = false;
        const refresh = this.refresh.bind(this);
        const onTargetChanged = () => {
            this._summaryWasGenerated = false;
        }
        const modelChanged = () => this.onChangeLlmModel();
        const apply = () => $timeout(() => $scope.$apply());
        dispatcher.on('target:identification:publications:page:changed', refresh);
        dispatcher.on('target:identification:changed', onTargetChanged);
        dispatcher.on('target:identification:publications:model:changed', modelChanged);
        dispatcher.on('target:identification:publications:chat:initialized', apply);
        dispatcher.on('target:identification:publications:chat:answer', apply);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:drugs:source:changed', refresh);
            dispatcher.removeListener('target:identification:changed', onTargetChanged);
            dispatcher.removeListener('target:identification:publications:model:changed', modelChanged);
            dispatcher.removeListener('target:identification:publications:chat:initialized', apply);
            dispatcher.removeListener('target:identification:publications:chat:answer', apply);
        });
    }

    get llmModel() {
        return this.targetLLMService
            ? this.targetLLMService.model
            : undefined;
    }

    set llmModel(llmModel) {
        if (this.targetLLMService) {
            this.targetLLMService.model = llmModel;
        }
    }

    get llmModelType() {
        return this.llmModel ? this.llmModel.type : undefined;
    }

    get llmModels() {
        return this.targetLLMService
            ? this.targetLLMService.models
            : [];
    }

    get publications() {
        return this.ngbBibliographyPanelService.publications;
    }

    get keyWords() {
        return this.ngbBibliographyPanelService.keyWords;
    }
    set keyWords(value) {
        this.ngbBibliographyPanelService.keyWords = value;
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
        this._summaryWasGenerated = true;
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }
        this.loadingSummary = true;
        this.summary = await this.ngbBibliographyPanelService.getLlmSummary()
            .then(success => {
                if (success) {
                    return this.ngbBibliographyPanelService.summaryResult;
                }
                return null;
            });
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeLlmModel() {
        if (this._summaryWasGenerated) {
            (this.generateSummary)();
        }
    }

    async searchPublications() {
        await this.ngbBibliographyPanelService.getDataOnPage(1);
        this.refresh();
    }

    onBlur () {
        if (this.searchText !== this.keyWords) {
            this.keyWords = this.searchText;
            this.searchPublications();
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
