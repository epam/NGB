import processLinks from '../../utilities/process-links';

const PAGE_SIZE = 5;

export default class NgbBibliographyPanelService {

    _loadingPublications = false;
    _failedPublications = false;
    _publicationsError = null;
    _emptyPublications = false;

    _loadingSummary = false;
    _failedSummary = false;
    _summaryError = null;
    _emptySummary = false;

    _publications = [];
    _totalPages = 0;
    _currentPage = 1;
    _totalPublications = 0;

    _summaryResult = null;

    _llmSummaryToken = 0;
    _publicationsToken = 0;

    _keyWords = '';
    _selectedGeneIds;

    get pageSize() {
        return PAGE_SIZE;
    }

    get loadingPublications() {
        return this._loadingPublications;
    }
    get failedPublications() {
        return this._failedPublications;
    }
    get publicationsError() {
        return this._publicationsError;
    }
    get emptyPublications() {
        return this._emptyPublications;
    }

    get loadingSummary() {
        return this._loadingSummary;
    }
    set loadingSummary(value) {
        this._loadingSummary = value;
    }
    get failedSummary() {
        return this._failedSummary;
    }
    get summaryError() {
        return this._summaryError;
    }

    get publications() {
        return this._publications || [];
    }

    get totalPages() {
        return this._totalPages;
    }
    get currentPage() {
        return this._currentPage;
    }
    set currentPage(value) {
        this._currentPage = value;
    }

    get summaryResult() {
        return this._summaryResult;
    }

    get keyWords() {
        return this._keyWords;
    }
    set keyWords(value) {
        this._keyWords = value;
    }
    get totalPublications() {
        return this._totalPublications;
    }
    get selectedGeneIds() {
        return this._selectedGeneIds || [];
    }
    set selectedGeneIds(value) {
        this._selectedGeneIds = value || [];
    }

    static instance (
        $sce,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        targetLLMService
    ) {
        return new NgbBibliographyPanelService(
            $sce,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            targetLLMService
        );
    }

    constructor(
        $sce,
        dispatcher,
        ngbTargetPanelService,
        targetDataService,
        targetLLMService
    ) {
        Object.assign(this, {
            $sce,
            dispatcher,
            ngbTargetPanelService,
            targetDataService,
            targetLLMService
        });
        this.selectedGeneIds = this.genes.map(g => g.geneId);

        dispatcher.on('target:identification:changed', this.updateGenes.bind(this));
        this.updateGenes(ngbTargetPanelService.identificationTarget);
    }

    get genes() {
        return this.ngbTargetPanelService.allGenes || [];
    }

    updateGenes (targetIdentificationData) {
        this.selectedGeneIds = this.genes.map(g => g.geneId);
        this.clearSummary();
        this.clearPublications();
        this._totalPublications = (this.ngbTargetPanelService.identificationData || {}).publicationsCount;
        (this.getPublicationsResults)(1);
    }

    async getDataOnPage(page) {
        const success = await this.getPublicationsResults(page);
        if (success) {
            this.currentPage = page;
            return true;
        }
    }

    getPublicationsResults(page) {
        if (!this.selectedGeneIds.length) {
            return new Promise(resolve => {
                this._loadingPublications = false;
                this.dispatcher.emit('target:identification:publications:loaded');
                this.dispatcher.emit('target:identification:publications:page:changed');
                this.dispatcher.emit('target:identification:publications:results:updated');
                resolve(false);
            });
        }
        this.dispatcher.emit('target:identification:publications:loading');
        this._loadingPublications = true;
        const commit = this._getPublicationsCommitPhase();
        return new Promise(resolve => {
            this.targetDataService.getPublications({
                geneIds: this.selectedGeneIds,
                page: page,
                pageSize: this.pageSize,
                keywords: this.keyWords
            })
                .then(([data, totalCount]) => {
                    commit(() => {
                        this._failedPublications = false;
                        this._publicationsError = null;
                        this._totalPages = Math.ceil(totalCount/this.pageSize);
                        this._totalPublications = totalCount;
                        this._emptyPublications = totalCount === 0;
                        this._publications = data;
                        this.searchedGeneIds = [...this.selectedGeneIds];
                        this._loadingPublications = false;
                        this.dispatcher.emit('target:identification:publications:loaded');
                        this.dispatcher.emit('target:identification:publications:page:changed');
                        this.dispatcher.emit('target:identification:publications:results:updated');
                    });
                    resolve(true);
                })
                .catch(err => {
                    commit(() => {
                        this._failedPublications = true;
                        this._publicationsError = [err.message];
                        this._emptyPublications = false;
                        this._loadingPublications = false;
                        this.dispatcher.emit('target:identification:publications:loaded');
                        this.dispatcher.emit('target:identification:publications:page:changed');
                        this.dispatcher.emit('target:identification:publications:results:updated');
                    });
                    resolve(false);
                });
        });
    }

    _increaseLLMSummaryToken() {
        this._llmSummaryToken = (this._llmSummaryToken || 0) + 1;
        return this._llmSummaryToken;
    }

    _increasePublicationsToken() {
        this._publicationsToken = (this._publicationsToken || 0) + 1;
        return this._publicationsToken;
    }

    _getLLMSummaryCommitPhase() {
        const token = this._increaseLLMSummaryToken();
        return (fn) => {
            if (typeof fn === 'function' && token === this._llmSummaryToken) {
                fn();
            }
        };
    }

    _getPublicationsCommitPhase() {
        const token = this._increasePublicationsToken();
        return (fn) => {
            if (typeof fn === 'function' && token === this._publicationsToken) {
                fn();
            }
        };
    }

    clearSummary() {
        this._increaseLLMSummaryToken();
        this._failedSummary = false;
        this._summaryError = null;
        this._summaryResult = undefined;
        this._loadingSummary = false;
    }

    clearPublications() {
        this._currentPage = 1;
        this._increasePublicationsToken();
        this._failedPublications = false;
        this._publicationsError = null;
        this._totalPages = 0;
        this._totalPublications = 0
        this._keyWords = '';
        this._emptyPublications = false;
        this._publications = [];
        this._loadingPublications = false;
    }

    getLlmSummary() {
        if (!this.targetLLMService || !this.targetLLMService.model) {
            return Promise.resolve();
        }
        const request = (this._publications || []).slice(0, 10).map(p => p.uid);
        const commit = this._getLLMSummaryCommitPhase();
        return new Promise(resolve => {
            this.targetDataService.getLlmSummary(request, this.targetLLMService.model)
                .then((data) => {
                    commit(() => {
                        this._failedSummary = false;
                        this._summaryError = null;
                        this.setSummaryResults(data);
                        this._loadingSummary = false;
                    });
                    resolve(true);
                })
                .catch(err => {
                    commit(() => {
                        this._failedSummary = true;
                        this._summaryError = [err.message];
                        this._loadingSummary = false;
                    });
                    resolve(false);
                });
        });
    }

    setSummaryResults(summary) {
        this._summaryResult = {
            html: this.$sce.trustAsHtml(processLinks(summary)),
            summary
        };
    }
}
