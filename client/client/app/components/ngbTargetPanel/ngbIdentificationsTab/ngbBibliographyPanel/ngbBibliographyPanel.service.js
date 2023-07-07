const PAGE_SIZE = 5;

export default class ngbBibliographyPanelService {

    _loadingPublications = false;
    _failedPublications = false;
    _publicationsError = null;
    _emptyPublications = false;

    _loadingSummary = false;
    _failedSummary = false;
    _summaryError = null;
    _emptySummary = false;

    _publicationsResults = null;
    _allPublications = null;
    _totalPages = 0;
    _currentPage = 1;

    _summaryResult = null;

    get pageSize() {
        return PAGE_SIZE;
    }

    get loadingPublications() {
        return this._loadingPublications;
    }
    set loadingPublications(value) {
        this._loadingPublications = value;
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

    get publicationsResults() {
        return this._publicationsResults;
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
    
    static instance ($sce, dispatcher, ngbTargetPanelService, targetDataService) {
        return new ngbBibliographyPanelService($sce, dispatcher, ngbTargetPanelService, targetDataService);
    }

    constructor($sce, dispatcher, ngbTargetPanelService, targetDataService) {
        Object.assign(this, {$sce, dispatcher, ngbTargetPanelService, targetDataService});
    }

    get geneIds() {
        return this.ngbTargetPanelService.allGenes.map(g => g.geneId);
    }

    setPublications() {
        const data = this._allPublications;
        const start = (this.currentPage - 1) * this.pageSize;
        const end = this.currentPage * this.pageSize;
        this._publicationsResults = data.slice(start, end);
        this._loadingPublications = false;
    }

    async getDataOnPage(page) {
        this._loadingPublications = true;
        this.currentPage = page;
        this.setPublications();
        this.dispatcher.emit('publication:page:changed');
    }

    getRequest() {
        return {
            geneIds: this.geneIds
        }
    }

    getPublicationsResults() {
        const request = this.getRequest();
        if (!request.geneIds || !request.geneIds.length) {
            return new Promise(resolve => {
                this._loadingPublications = false;
                resolve(true);
            });
        }
        return new Promise(resolve => {
            this.targetDataService.getPublications(request)
                .then(([data, totalCount]) => {
                    this._failedPublications = false;
                    this._publicationsError = null;
                    this._totalPages = Math.ceil(totalCount/this.pageSize);
                    this._emptyPublications = totalCount === 0;
                    this._allPublications = data;
                    this.setPublications();
                    this._loadingPublications = false;
                    resolve(true);
                })
                .catch(err => {
                    this._failedPublications = true;
                    this._publicationsError = [err.message];
                    this._totalPages = 0;
                    this._emptyPublications = false;
                    this._loadingPublications = false;
                    resolve(false);
                });
        });
    }

    getLlmSummary(provider) {
        const request = this._allPublications.slice(0, 10).map(p => p.uid);
        return new Promise(resolve => {
            this.targetDataService.getLlmSummary(request, provider)
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
        const getSummaryElements = (summary) => {
            const html = this.$sce.trustAsHtml(summary);
            const breakRegex = /[\n]/gi;
            let match;
            let startIndex = 0;
            const elements = [];

            while ((match = breakRegex.exec(html)) !== null) {
                elements.push(match.input.substring(startIndex, match.index));
                startIndex = match.index + match[0].length;
            }
            if (match === null && startIndex < summary.length) {
                elements.push(summary.substring(startIndex));
            }
            return elements;
        };
        this._summaryResult = getSummaryElements(summary);
    }
}
