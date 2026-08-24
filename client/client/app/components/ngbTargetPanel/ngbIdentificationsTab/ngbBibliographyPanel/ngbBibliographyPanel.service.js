const PAGE_SIZE = 5;

export default class NgbBibliographyPanelService {

    _loadingPublications = false;
    _failedPublications = false;
    _publicationsError = null;
    _emptyPublications = false;

    _publications = [];
    _totalPages = 0;
    _currentPage = 1;
    _totalPublications = 0;

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
        dispatcher,
        ngbTargetPanelService,
        targetDataService
    ) {
        return new NgbBibliographyPanelService(
            dispatcher,
            ngbTargetPanelService,
            targetDataService
        );
    }

    constructor(
        dispatcher,
        ngbTargetPanelService,
        targetDataService
    ) {
        Object.assign(this, {
            dispatcher,
            ngbTargetPanelService,
            targetDataService
        });
        this.selectedGeneIds = this.genes.map(g => g.geneId);

        dispatcher.on('target:identification:changed', this.updateGenes.bind(this));
        this.updateGenes(ngbTargetPanelService.identificationTarget);
    }

    get genes() {
        return this.ngbTargetPanelService.allGenes || [];
    }

    get targetId() {
        const {target} = this.ngbTargetPanelService.identificationTarget || {};
        return target.id;
    }

    updateGenes (targetIdentificationData) {
        this.selectedGeneIds = this.genes.map(g => g.geneId);
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

    getPublicationRequest(page) {
        const request = {
            geneIds: this.selectedGeneIds,
            page: page,
            pageSize: this.pageSize,
            keywords: this.keyWords
        };
        if (this.targetId) {
            request.targetId = this.targetId;
        }
        return request;
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
            const request = this.getPublicationRequest(page);
            this.targetDataService.getPublications(request)
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

    _increasePublicationsToken() {
        this._publicationsToken = (this._publicationsToken || 0) + 1;
        return this._publicationsToken;
    }

    _getPublicationsCommitPhase() {
        const token = this._increasePublicationsToken();
        return (fn) => {
            if (typeof fn === 'function' && token === this._publicationsToken) {
                fn();
            }
        };
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

}
