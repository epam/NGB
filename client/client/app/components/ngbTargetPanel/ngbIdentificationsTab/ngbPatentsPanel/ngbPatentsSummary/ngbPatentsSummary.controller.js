import processLinks from '../../../utilities/process-links';

export default class ngbPatentsSummaryController {
    static get UID() {
        return 'ngbPatentsSummaryController';
    }
    _loading = false;
    _errors = undefined;
    _summary = undefined;

    constructor($scope, $sce, $timeout, dispatcher, ngbPatentsPanelService, targetLLMService, targetDataService) {
        Object.assign(this, { $scope, $sce, $timeout, dispatcher, ngbPatentsPanelService, targetLLMService, targetDataService });
        dispatcher.on('target:identification:patents:source:changed', this.reset.bind(this));
        this.reset();
    }

    get loading() {
        return this._loading;
    }

    get errors() {
        return this._errors;
    }

    get summary() {
        return this._summary;
    }

    get llmModel() {
        return this.targetLLMService
            ? this.targetLLMService.model
            : undefined;
    }

    get llmModels() {
        return this.targetLLMService
            ? this.targetLLMService.models
            : [];
    }

    get available() {
        return this.llmModel && this.llmModels && this.search && typeof this.search === 'string' && this.search.trim().length > 0;
    }

    async onSearch() {
        this._token = {};
        const token = this._token;
        const commit = (fn) => {
            if (token === this._token) {
                fn();
            }
        };
        try {
            this._loading = true;
            this._summary = undefined;
            if (!this.llmModel) {
                throw new Error('LLM model not specified');
            }
            const result = await this.targetDataService.getGooglePatentsSummary(this.search, this.llmModel);
            commit(() => {
                this._summary = {
                    result,
                    html: this.$sce.trustAsHtml(processLinks(result)),
                }
            });
        } catch (error) {
            commit(() => {
                this._errors = [error.message];
            });
        } finally {
            commit(() => {
                this._loading = false;
            });
        }
        this.$timeout(() => {
            this.$scope.$apply();
        });
    }

    reset() {
        this._token = {};
        this._loading = false;
        this._errors = undefined;
        this._summary = undefined;
    }
}