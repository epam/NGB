const DEFAULT_COLUMNS = ['Gene ID', 'Gene Name', 'Tax ID', 'Species Name', 'Priority'];
const REMOVE = 'Remove';

const DISPLAY_NAME = {
    geneId: 'Gene ID',
    geneName: 'Gene Name',
    taxId: 'Tax ID',
    speciesName: 'Species Name',
    priority: 'Priority',
    remove: ''
};

const COLUMN_FIELD = {
    'Gene ID': 'geneId',
    'Gene Name': 'geneName',
    'Tax ID': 'taxId',
    'Species Name': 'speciesName',
    'Priority': 'priority',
    'Remove': 'remove'
};

const PAGE_SIZE = 20;

export default class ngbTargetGenesTableService {

    get defaultColumns() {
        return DEFAULT_COLUMNS;
    }

    get pageSize() {
        return PAGE_SIZE;
    }

    get displayName() {
        return DISPLAY_NAME;
    }

    get columnField() {
        return COLUMN_FIELD;
    }

    get removeColumn() {
        return REMOVE;
    }

    _tableResults = null;
    _displayFilters = false;

    _totalPages = 0;
    _currentPage = 1;

    additionalColumns = [];

    get displayFilters() {
        return this._displayFilters;
    }
    set displayFilters(value) {
        this._displayFilters = value;
    }
    get totalPages() {
        return this._totalPages;
    }
    set totalPages(value) {
        this._totalPages = value;
    }
    get currentPage() {
        return this._currentPage;
    }
    set currentPage(value) {
        this._currentPage = value;
    }

    get currentColumns () {
        return [...this.defaultColumns, ...this.additionalColumns, this.removeColumn];
    }
    get currentColumnFields() {
        return this.currentColumns.map(c => this.columnField[c] || c)
    }

    static instance (dispatcher, ngbTargetsTabService, ngbTargetPanelService, targetContext) {
        return new ngbTargetGenesTableService(dispatcher, ngbTargetsTabService, ngbTargetPanelService, targetContext);
    }

    constructor(dispatcher, ngbTargetsTabService, ngbTargetPanelService, targetContext) {
        Object.assign(this, {dispatcher, ngbTargetsTabService, ngbTargetPanelService, targetContext});
        dispatcher.on('target:model:changed', this.resetTargetModel.bind(this));
    }

    get isParasiteType() {
        return this.ngbTargetsTabService.targetModel.type === this.targetType.PARASITE;
    }

    get tableResults() {
        if (this.isParasiteType && this.currentPage === this.totalPages) {
            return [...this._tableResults, ...this.ngbTargetsTabService.addedGenes];
        } else {
            return this._tableResults;
        }
    }

    set loading(value) {
        this.ngbTargetsTabService.formLoading = value;
    }

    get targetType() {
        return this.targetContext.targetType;
    }

    getColumnName(field) {
        return this.displayName[field];
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize
        };
        return request;
    }

    async getTableResults() {
        if (this.isParasiteType) {
            const request = this.getRequest();
            const id = this.ngbTargetsTabService.targetModel.id;
            this._tableResults = await this.ngbTargetsTabService.getTargetGenes(id, request)
                .then(success => {
                    if (success) {
                        return this.ngbTargetsTabService.targetModel.genes;
                    }
                    return [];
                });
            this.totalPages = Math.ceil(this.ngbTargetsTabService.targetModel.genesTotal/this.pageSize);
            this.loading = false;
            return Promise.resolve(true);
        } else {
            return new Promise(resolve => {
                this._tableResults = this.ngbTargetsTabService.targetModel.genes;
                this.loading = false;
                resolve(true);
            });
        }
    }

    resetTargetModel() {
        this._tableResults = null;
        this._displayFilters = false;
        this._totalPages = 0;
        this._currentPage = 1;
    }

    async onChangeShowFilters() {}
}
