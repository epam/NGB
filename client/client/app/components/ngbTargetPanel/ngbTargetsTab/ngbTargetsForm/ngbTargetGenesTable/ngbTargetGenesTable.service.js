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

const FILTER_TYPE = {
    TERM: 'TERM',
    PHRASE: 'PHRASE',
    OPTIONS: 'OPTIONS',
    RANGE: 'RANGE',
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

    get filterType() {
        return FILTER_TYPE;
    }

    _tableResults = null;
    _originalResults = null;
    _displayFilters = false;

    _totalPages = 0;
    _currentPage = 1;

    additionalColumns = [];
    allColumns = [];
    _columnsInfo = {};

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

    get columnsInfo() {
        return this._columnsInfo;
    }
    set columnsInfo(value) {
        if (!value || !value.length) return;
        if (!this._columnsInfo) {
            this._columnsInfo = {};
        }
        for (let i = 0; i < value.length; i++) {
            const column = value[i];
            this._columnsInfo[column.fieldName] = {
                filterType: value[i].filterType,
                sort: value[i].sort,
            };
        }
    }

    async initAdditionalColumns() {
        if (this.isParasiteType) {
            const savedColumns = JSON.parse(localStorage.getItem('targetGenesColumns'));
            const availableColumns = await this.getMetadataColumns();
            if (savedColumns && savedColumns.length) {
                const columns = savedColumns.filter(c => availableColumns.includes(c));
                this.setAdditionalColumns(columns);
            }
            this.dispatcher.emit('target:form:table:columns');
        }
    }

    async getMetadataColumns() {
        return await this.ngbTargetsFormService.setTargetGenesFields()
            .then(columns => {
                this.allColumns = columns;
                this.columnsInfo = columns;
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
                .map(([key, values]) => {
                    const field = this.getColumnName(key);
                    const type = this.getColumnFilterType(field);
                    if (type === this.filterType.RANGE) {
                        const {from, to} = values[0];
                        const range = {};
                        if (from) range.from = from;
                        if (to) range.to = to;
                        return { field, range };
                    }
                    if (type === this.filterType.OPTIONS) {
                        return {
                            field,
                            terms: values.map(v => v)
                        };
                    }
                    if (type === this.filterType.TERM || type === this.filterType.PHRASE) {
                        return { field, terms: [values]}
                    }
            });
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
        this._columnsInfo = {};
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
        this._columnsInfo = {};
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

    getIsColumnSort(columnName) {
        const column = this.columnsInfo[columnName];
        return column ? column.sort : false;
    }

    getColumnFilterType(columnName) {
        const column = this.columnsInfo[columnName];
        return column ? column.filterType : 'TERM';
    }

    getGeneFieldValues(field) {
        const targetId = this.targetModel.id;
        const fieldName = this.getColumnField(field);
        const filterType = this.getColumnFilterType(field);
        if (filterType !== this.filterType.OPTIONS) {
            return Promise.resolve(false);
        }
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
        const filterType = this.getColumnFilterType(this.getColumnName(field));
        if (value && value.length) {
            if (filterType === this.filterType.RANGE) {
                const {from, to} = value[0];
                if (!from && !to) {
                    delete filter[field];
                } else {
                    filter[field] = value;
                }
            } else {
                filter[field] = value;
            }
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
