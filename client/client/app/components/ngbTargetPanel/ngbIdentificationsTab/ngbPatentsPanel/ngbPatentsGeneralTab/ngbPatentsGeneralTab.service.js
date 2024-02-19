import {mapGooglePatentsData} from '../utilities/map-google-patents-data';

export default class ngbPatentsGeneralTabService {
    static instance(dispatcher, ngbPatentsPanelService, ngbTargetPanelService, targetDataService) {
        return new ngbPatentsGeneralTabService(dispatcher, ngbPatentsPanelService, ngbTargetPanelService, targetDataService);
    }

    _loading = false;
    _search = '';
    _errors = undefined;
    _page = 0;
    _pageSize = 10;
    _results = [];
    _total = 0;
    constructor(dispatcher, ngbPatentsPanelService, ngbTargetPanelService, targetDataService) {
        Object.assign(this, { dispatcher, ngbPatentsPanelService, ngbTargetPanelService, targetDataService });
        this._loading = false;
        this._search = '';
        this._errors = undefined;
        this._results = [];
        this._total = 0;
        dispatcher.on('target:identification:patents:source:changed', this.resetTargetIdentificationData.bind(this));
        dispatcher.on('target:identification:changed', this.resetTargetIdentificationData.bind(this));
        this.resetTargetIdentificationData();
    }

    get loading() {
        return this._loading;
    }

    get search() {
        return this._search;
    }

    set search(search) {
        this._search = search;
    }

    get errors() {
        return this._errors;
    }

    get page() {
        return this._page;
    }

    get pageSize() {
        return this._pageSize;
    }

    get total() {
        return this._total;
    }

    get totalPages() {
        return Math.ceil(this.total / this.pageSize);
    }

    get hasMore() {
        return this.page + 1 < this.totalPages;
    }

    get results() {
        return this._results;
    }

    resetTargetIdentificationData() {
        this._token = {};
        this._loading = false;
        this._errors = undefined;
        this._total = 0;
        this._page = 0;
        this._results = [];
        if (this.ngbTargetPanelService) {
            const id = this.ngbTargetPanelService.geneIdsOfInterest[0];
            const genes = this.ngbTargetPanelService.allGenes.slice();
            let gene = genes[0];
            if (id) {
                gene = genes.find((g) => g.geneId === id) || genes[0];
            }
            if (gene) {
                this.search = gene.geneName;
            }
        }
    }

    async performSearch() {
        this._results = [];
        await this.getPageData(0);
    }

    async getPageData(page) {
        this._token = {};
        const token = this._token;
        const commitChanges = (fn) => {
            if (token === this._token) {
                fn();
            }
        };
        this._page = page;
        this._loading = true;
        try {
            this._errors = undefined;
            const result = await this.targetDataService.getGooglePatentsByName({
                name: this.search,
                page: page + 1,
                pageSize: this.pageSize
            });
            const {
                items = [],
                totalCount = 0
            } = result || {};
            commitChanges(() => {
                this._total = totalCount;
                this._results = items.map(mapGooglePatentsData);
            });
        } catch (error) {
            commitChanges(() => {
                this._errors = [error.message];
            });
        } finally {
            commitChanges(() => {
                this._loading = false;
            });
        }
    }
}