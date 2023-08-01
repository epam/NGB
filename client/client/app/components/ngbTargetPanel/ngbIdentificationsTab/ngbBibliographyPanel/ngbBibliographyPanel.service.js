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

    _summaryResult = null;

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
        this._genes = [];

        dispatcher.on('target:identification:changed', this.updateGenes.bind(this));
        this.updateGenes(ngbTargetPanelService.identificationTarget);
    }

    get genes() {
        return this._genes;
    }

    updateGenes (targetIdentificationData) {
        const {
            interest = [],
            translational = []
        } = targetIdentificationData || {};
        this._genes = [...new Set([
            ...interest.map(g => g.geneId),
            ...translational.map(g => g.geneId),
        ])];
        (this.getPublicationsResults)();
    }

    async getDataOnPage(page) {
        this.currentPage = page;
        this.getPublicationsResults();
    }

    getPublicationsResults() {
        if (!this.genes.length) {
            return new Promise(resolve => {
                this._loadingPublications = false;
                this.dispatcher.emit('target:identification:publications:loaded');
                this.dispatcher.emit('target:identification:publications:page:changed');
                resolve(true);
            });
        }
        this.dispatcher.emit('target:identification:publications:loading');
        this._loadingPublications = true;
        return new Promise(resolve => {
            this.targetDataService.getPublications({
                geneIds: this.genes,
                page: this.currentPage,
                pageSize: this.pageSize
            })
                .then(([data, totalCount]) => {
                    this._failedPublications = false;
                    this._publicationsError = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyPublications = totalCount === 0;
                    this._publications = data;
                    this._loadingPublications = false;
                    this.dispatcher.emit('target:identification:publications:loaded');
                    this.dispatcher.emit('target:identification:publications:page:changed');
                    resolve(true);
                })
                .catch(err => {
                    this._failedPublications = true;
                    this._publicationsError = [err.message];
                    this._totalPages = 0;
                    this._emptyPublications = false;
                    this._loadingPublications = false;
                    this.dispatcher.emit('target:identification:publications:loaded');
                    this.dispatcher.emit('target:identification:publications:page:changed');
                    resolve(false);
                });
        });
    }

    getLlmSummary() {
        if (!this.targetLLMService || !this.targetLLMService.model) {
            return Promise.resolve();
        }
        const request = (this._publications || []).slice(0, 10).map(p => p.uid);
        return new Promise(resolve => {
            this.targetDataService.getLlmSummary(request, this.targetLLMService.model)
                .then((data) => {
                    this._failedSummary = false;
                    this._summaryError = null;
                    this.setSummaryResults(data);
                    this._loadingSummary = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedSummary = true;
                    this._summaryError = [err.message];
                    this._loadingSummary = false;
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
