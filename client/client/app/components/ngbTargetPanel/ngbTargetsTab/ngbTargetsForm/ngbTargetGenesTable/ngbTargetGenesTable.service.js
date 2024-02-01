import {encodeName, decodeName} from '../ngbTargetsForm.service';

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

export default class ngbTargetGenesTableService {

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
    _originalResults = null;
    _displayFilters = false;

    _totalPages = 0;
    _currentPage = 1;

    additionalColumns = [];
    allColumns = [];

    _sortInfo = null;
    _filterInfo = null;
    fieldList = {};

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
    get sortInfo() {
        return this._sortInfo;
    }
    set sortInfo(value) {
        this._sortInfo = value;
    }
    get filterInfo() {
        return this._filterInfo;
    }
    get defaultColumns() {
        return this.ngbTargetsFormService.defaultFields;
    }

    get currentColumns () {
        return [...this.defaultColumns, ...this.additionalColumns, this.removeColumn];
    }

    get currentColumnFields() {
        return this.currentColumns.map(c => {
            if (this.columnField[c]) return this.columnField[c];
            return encodeName(c);
        })
    }

    async initAdditionalColumns() {
        const savedColumns = JSON.parse(localStorage.getItem('targetGenesColumns'));
        const availableColumns = await this.getMetadataColumns();
        if (savedColumns && savedColumns.length) {
            const columns = savedColumns.filter(c => availableColumns.includes(c));
            this.setAdditionalColumns(columns);
        }
        this.dispatcher.emit('target:form:table:columns');
    }

    async getMetadataColumns() {
        return await this.ngbTargetsFormService.setTargetGenesFields()
            .then(columns => {
                this.allColumns = columns;
                return this.ngbTargetsFormService.metadataFields;
            })
    }

    setAdditionalColumns(columns) {
        this.additionalColumns = [...columns];
        localStorage.setItem('targetGenesColumns', JSON.stringify(columns));
    }

    static instance (
        dispatcher,
        ngbTargetsFormService,
        ngbTargetsTabService,
        ngbTargetPanelService,
        targetDataService,
        targetContext,
    ) {
        return new ngbTargetGenesTableService(
            dispatcher,
            ngbTargetsFormService,
            ngbTargetsTabService,
            ngbTargetPanelService,
            targetDataService,
            targetContext,
        );
    }

    constructor(
        dispatcher,
        ngbTargetsFormService,
        ngbTargetsTabService,
        ngbTargetPanelService,
        targetDataService,
        targetContext,
    ) {
        Object.assign(this, {
            dispatcher,
            ngbTargetsFormService,
            ngbTargetsTabService,
            ngbTargetPanelService,
            targetDataService,
            targetContext,
        });
        dispatcher.on('target:model:changed', this.resetTargetModel.bind(this));
        dispatcher.on('target:model:type:changed', this.changeType.bind(this));
    }

    get isParasiteType() {
        return this.targetModel.type === this.targetType.PARASITE;
    }

    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    get tableResults() {
        if (this.isParasiteType) {
            if (this.currentPage === this.totalPages) {
                return [...this._tableResults, ...this.ngbTargetsFormService.addedGenes];
            }
            if (this.isAddMode && !this.totalPages && (this._tableResults && !this._tableResults.length)) {
                return [...this.ngbTargetsFormService.addedGenes];
            }
            return this._tableResults;
        } else {
            return this._tableResults;
        }
    }

    set loading(value) {
        this.ngbTargetsFormService.loading = value;
    }

    get targetType() {
        return this.targetContext.targetType;
    }
    get targetModel() {
        return this.ngbTargetsFormService.targetModel;
    }

    get pageSize() {
        return this.ngbTargetsFormService.pageSize;
    }

    resetTableResults() {
        this._tableResults = this._originalResults.map(g => ({...g}));
    }

    getColumnName(field) {
        if (Object.prototype.hasOwnProperty.call(this.displayName, field)) {
            return this.displayName[field];
        }
        return decodeName(field);
    }

    getColumnField(name) {
        if (Object.prototype.hasOwnProperty.call(this.columnField, name)) {
            return this.columnField[name];
        }
        return name;
    }

    getRequest() {
        const request = {
            page: this.currentPage,
            pageSize: this.pageSize
        };
        if (this.sortInfo && this.sortInfo.length) {
            request.orderInfos = this.sortInfo.map(i => ({
                orderBy: this.getColumnName(i.field),
                reverse: !i.ascending
            }))
        }
        if (this._filterInfo) {
            const filters = Object.entries(this._filterInfo)
                .filter(([key, values]) => values.length)
                .map(([key, values]) => ({
                    field: this.getColumnName(key),
                    terms: Array.isArray(values) ? values.map(v => v) : values
                }));
            if (filters && filters.length) {
                request.filters = filters;
            }
        }
        return request;
    }

    async getTableResults() {
        if (this.isParasiteType) {
            const request = this.getRequest();
            const id = this.targetModel.id;
            this._tableResults = await this.ngbTargetsFormService.getTargetGenes(id, request)
                .then(success => {
                    if (success) {
                        return this.targetModel.genes;
                    }
                    return [];
                });
            this.setOriginalResults(this._tableResults);
            this.totalPages = Math.ceil(this.targetModel.genesTotal/this.pageSize);
            this.loading = false;
            return Promise.resolve(true);
        } else {
            return new Promise(resolve => {
                this._tableResults = this.targetModel.genes;
                this.loading = false;
                resolve(true);
            });
        }
    }

    setOriginalResults(results) {
        this._originalResults = results.map(g => ({...g}))
    }

    restoreView() {
        if (this._sortInfo || this._filterInfo) {
            this._sortInfo = null;
            this._filterInfo = null;
            this.dispatcher.emit('target:form:sort:reset');
            this._tableResults = null;
            this._originalResults = null;
            this.dispatcher.emit('target:form:filters:changed');
        }
        this.additionalColumns = [];
        this.allColumns = [];
        this._displayFilters = false;
    }

    resetTargetModel() {
        this._tableResults = null;
        this._originalResults = null;
        this._displayFilters = false;
        this._totalPages = 0;
        this._currentPage = 1;
        this._sortInfo = null;
        this._filterInfo = null;
        this.fieldList = {};
        this.additionalColumns = [];
        this.allColumns = [];
    }

    async onChangeShowFilters() {
        if (this.displayFilters) {
            const promises = this.currentColumns
                .filter(c => c !== this.removeColumn)
                .map(field => this.getGeneFieldValues(field));
            return Promise.allSettled(promises)
                .then(values => values.some(v => v))
                .catch(err => false)
        }
    }

    getGeneFieldValues(field) {
        const targetId = this.targetModel.id;
        const fieldName = this.getColumnField(field);
        return new Promise(resolve => {
            this.targetDataService.getGenesFieldValue(targetId, field)
                .then((data) => {
                    this.fieldList[fieldName] = data.filter(d => d);
                    resolve(true);
                })
                .catch(err => {
                    this.fieldList[fieldName] = [];
                    resolve(false);
                });
        });
    }

    async setFilterList(column) {
        if (!this.displayFilters) return;
        await this.getGeneFieldValues(column)
            .then(result => {
                if (result) {
                    this.dispatcher.emit('target:form:filters:list');
                }
            });
    }

    setFilter(field, value) {
        const filter = {...(this._filterInfo || {})};
        if (value && value.length) {
            filter[field] = value;
        } else {
            delete filter[field];
        }
        this._filterInfo = filter;
    }

    changeType() {
        this._tableResults = this.targetModel.genes;
        this.setOriginalResults(this.targetModel.genes);
    }
}
