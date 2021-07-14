const DEFAULT_GENES_COLUMNS = [
    'chr', 'gene', 'gene_id', 'type', 'start', 'end', 'strand', 'info'//, 'molecularView'
];
const OPTIONAL_GENE_COLUMNS = [
    'featureFileId', 'source', 'score', 'frame'
];
const DEFAULT_ORDERBY_GENES_COLUMNS = {
    'chr': 'CHROMOSOME_NAME',
    'gene': 'FEATURE_NAME',
    'gene_id': 'FEATURE_ID',
    'type': 'FEATURE_TYPE',
    'start': 'START_INDEX',
    'end': 'END_INDEX',
    'strand': 'strand'
};
const GENES_COLUMN_TITLES = {
    chr: 'Chr',
    gene: 'Name',
    gene_id: 'Id',
    source: 'Gene Source',
    type: 'Type',
    start: 'Start',
    end: 'End',
    strand: 'Strand',
    info: 'Info',
    molecularView: 'Molecular View'
};
const GENE_TYPE_LIST = [
    {label: 'GENE', value: 'GENE'},
    {label: 'MRNA', value: 'MRNA'},
    {label: 'RRNA', value: 'RRNA'},
    {label: 'CDS', value: 'CDS'},
    {label: 'EXON', value: 'EXON'},
    {label: 'UTR5', value: 'UTR5'},
    {label: 'UTR3', value: 'UTR3'},
    {label: 'UTR', value: 'UTR'},
    {label: 'NCRNA', value: 'NCRNA'},
    {label: 'TMRNA', value: 'TMRNA'},
    {label: 'TRNA', value: 'TRNA'},
    {label: 'OPERON', value: 'OPERON'},
    {label: 'REGION', value: 'REGION'},
    {label: 'REGULATORY', value: 'REGULATORY'},
    {label: 'GENERIC_GENE_FEATURE', value: 'GENERIC_GENE_FEATURE'},
    {label: 'START_CODON', value: 'START_CODON'},
    {label: 'STOP_CODON', value: 'STOP_CODON'},
];
const GENE_TYPE_COLOR = {
    GENE: '100',
    MRNA: '200',
    RRNA: '300',
    CDS: '400',
    EXON: '500',
    UTR5: '600',
    UTR3: '700',
    UTR: '800',
    NCRNA: '900',
    TMRNA: 'A00',
    TRNA: 'B00',
    OPERON: 'C00',
    REGION: 'D00',
    REGULATORY: 'E00',
    GENERIC_GENE_FEATURE: 'A700',
    START_CODON: 'DEL',
    STOP_CODON: 'INV',
};
const PAGE_SIZE = 100;
const blockFilterGenesTimeout = 500;

export default class ngbGenesTableService {

    _hasMoreGenes = true;
    _blockFilterGenes;

    constructor(dispatcher, genomeDataService, projectContext, uiGridConstants) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.projectContext = projectContext;
        this.uiGridConstants = uiGridConstants;
        this.initEvents();
        this.initialize();
    }

    _nextPageMarker = undefined;

    get nextPageMarker() {
        return this._nextPageMarker;
    }

    set nextPageMarker(value) {
        this._nextPageMarker = value;
    }

    get hasMoreData() {
        return this._hasMoreGenes;
    }

    get genesPageSize() {
        return PAGE_SIZE;
    }

    get geneTypeColor() {
        return GENE_TYPE_COLOR;
    }

    get geneTypeList() {
        return GENE_TYPE_LIST;
    }

    _genesTableError = null;

    get genesTableError() {
        return this._genesTableError;
    }

    _orderByGenes = null;

    get orderByGenes() {
        return this._orderByGenes;
    }

    set orderByGenes(orderByGenes) {
        this._orderByGenes = orderByGenes;
    }

    _genesFilterIsDefault = true;

    get genesFilterIsDefault() {
        return this._genesFilterIsDefault;
    }

    _displayGenesFilter;

    get displayGenesFilter() {
        if (this._displayGenesFilter !== undefined) {
            return this._displayGenesFilter;
        } else {
            this._displayGenesFilter = JSON.parse(localStorage.getItem('displayGenesFilter')) || false;
            return this._displayGenesFilter;
        }
    }

    _genesFilter = {
        additionalFilters: {}
    };

    get genesFilter() {
        return this._genesFilter;
    }

    get genesColumnTitleMap() {
        return GENES_COLUMN_TITLES;
    }

    get orderByColumnsGenes() {
        return DEFAULT_ORDERBY_GENES_COLUMNS;
    }

    get defaultGenesColumns() {
        return DEFAULT_GENES_COLUMNS;
    }

    get genesTableColumns() {
        if (!localStorage.getItem('genesTableColumns') || localStorage.getItem('genesTableColumns') === '[]') {
            localStorage.setItem('genesTableColumns', JSON.stringify(DEFAULT_GENES_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('genesTableColumns'));
    }

    set genesTableColumns(columns) {
        localStorage.setItem('genesTableColumns', JSON.stringify(columns || []));
    }

    _optionalGenesColumns = [];

    get optionalGenesColumns() {
        return this._optionalGenesColumns;
    }

    static instance(dispatcher, genomeDataService, projectContext, uiGridConstants) {
        return new ngbGenesTableService(dispatcher, genomeDataService, projectContext, uiGridConstants);
    }

    initEvents() {
        this.dispatcher.on('genes:reset:filter', this.resetGenesFilter.bind(this));
        this.dispatcher.on('reference:change', this.initialize.bind(this));
    }

    initialize() {
        if (!this.projectContext.reference) {
            return;
        }
        this.genomeDataService.getGenesInfo(this.projectContext.reference.id).then(data => {
            this._optionalGenesColumns = OPTIONAL_GENE_COLUMNS.concat(data.availableFilters);
            this.dispatcher.emit('genes:info:loaded');
        });
    }

    setDisplayGenesFilter(value, updateScope = true) {
        if (value !== this._displayGenesFilter) {
            this._displayGenesFilter = value;
            localStorage.setItem('displayGenesFilter', JSON.stringify(value));
            this.dispatcher.emitSimpleEvent('display:genes:filter', updateScope);
        }
    }

    async loadGenes(reference, page) {
        const filter = {
            chromosomeIds: this.genesFilter.chromosome || [],
            startIndex: this.genesFilter.start,
            endIndex: this.genesFilter.end,
            featureNames: this.genesFilter.gene || [],
            featureId: this.genesFilter.gene_id,
            featureTypes: this.genesFilter.type || [],
            additionalFilters: this.genesFilter.additionalFilters || {},
            attributesFields: this.genesTableColumns.filter(c => !this.defaultGenesColumns.includes(c)),
            pageSize: this.genesPageSize,
            pointer: page,
            orderBy: (this.orderByGenes || []).map(config => ({
                field: config.field,
                desc: !config.ascending
            }))
        };
        if (this.genesFilter.frame) {
            filter.frames = [this.genesFilter.frame];
        }
        if (this.genesFilter.source) {
            filter.sources = [this.genesFilter.source];
        }
        if (this.genesFilter.strand) {
            switch(this.genesFilter.strand) {
                case '+': {
                    filter.strands = ['POSITIVE'];
                    break;
                }
                case '-': {
                    filter.strands = ['NEGATIVE'];
                    break;
                }
            }
        }
        if (this.genesFilter.score) {
            filter.score = {
                left: this.genesFilter.score[0],
                right: this.genesFilter.score[1]
            };
        }
        this.refreshGenesFilterEmptyStatus();
        try {
            const data = await this.genomeDataService.loadGenes(
                reference,
                filter
            );
            this._genesTableError = null;
            this._hasMoreGenes = !!data.pointer;
            this.nextPageMarker = data.pointer;
            let filteredData = [];
            if (data.entries) {
                filteredData = (data.entries || [])
                    .map(this._formatServerToClient.bind(this))
                    .map(feature => ({...feature, referenceId: reference}));
            }
            return filteredData;
        } catch (e) {
            this._hasMoreGenes = false;
            this._genesTableError = e.message;
            return [];
        }
    }

    getGenesGridColumns() {
        const infoCell = require('./ngbGenesTable_info.tpl.html');
        const molecularViewCell = require('./ngbGenesTable_molecularView.tpl.html');
        const headerCells = require('./ngbGenesTable_header.tpl.html');

        const result = [];
        const columnsList = this.genesTableColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderByGenes) {
                const fieldName = (this.orderByColumnsGenes[column] || column);
                const [columnSortingConfiguration] = this.orderByGenes.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderByGenes.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'info': {
                    columnSettings = {
                        cellTemplate: infoCell,
                        enableSorting: false,
                        enableHiding: false,
                        enableFiltering: false,
                        enableColumnMenu: false,
                        field: 'id',
                        maxWidth: 70,
                        minWidth: 60,
                        name: this.genesColumnTitleMap[column]
                    };
                    break;
                }
                case 'molecularView': {
                    columnSettings = {
                        cellTemplate: molecularViewCell,
                        enableSorting: false,
                        enableHiding: false,
                        enableFiltering: false,
                        enableColumnMenu: false,
                        field: 'id',
                        maxWidth: 70,
                        minWidth: 60,
                        name: this.genesColumnTitleMap[column]
                    };
                    break;
                }
                case 'type': {
                    columnSettings = {
                        cellTemplate: `<div class="md-label variation-type"
                                    md-colors="{background: 'accent-{{grid.appScope.$ctrl.geneTypeColor[COL_FIELD]}}',color:'background-900'}"
                                    ng-class="COL_FIELD CUSTOM_FILTERS" >{{row.entity.feature}}</div>`,
                        enableHiding: false,
                        field: 'type',
                        filter: {
                            selectOptions: GENE_TYPE_LIST,
                            term: '',
                            type: this.uiGridConstants.filter.SELECT
                        },
                        headerCellTemplate: headerCells,
                        maxWidth: 104,
                        minWidth: 104,
                        name: 'Type',
                        filterApplied: () => this.genesFieldIsFiltered(column),
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.clearGeneFieldFilter(column),
                                shown: () => this.genesFieldIsFiltered(column)
                            }
                        ]
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: !this.defaultGenesColumns.includes(column),
                        enableFiltering: true,
                        enableSorting: true,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.genesColumnTitleMap[column] || column,
                        filterApplied: () => this.genesFieldIsFiltered(column),
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.clearGeneFieldFilter(column),
                                shown: () => this.genesFieldIsFiltered(column)
                            }
                        ],
                        width: '*'
                    };
                    break;
                }
            }
            if (columnSettings) {
                if (sortDirection) {
                    columnSettings.sort = {
                        direction: sortDirection,
                        priority: sortingPriority
                    };
                }
                result.push(columnSettings);
            }
        }
        return result;
    }

    _formatServerToClient(search) {
        const result = {
            ...search,
            ...search.attributes,
            chr: search.chromosome ? search.chromosome.name : undefined,
            gene: search.featureName,
            gene_id: search.featureId,
            start: search.startIndex,
            end: search.endIndex,
            type: search.featureType
        };
        delete result.attributes;
        return result;
    }

    refreshGenesFilterEmptyStatus() {
        const {additionalFilters, ...defaultFilters} = this.genesFilter;
        const additionalFiltersAreEmpty = Object.entries(additionalFilters).every(field => !field[1]);
        const defaultFiltersAreEmpty = Object.entries(defaultFilters).every(field => field[1] === undefined);
        this.projectContext.genesFilterIsDefault = additionalFiltersAreEmpty && defaultFiltersAreEmpty;
    }

    genesFieldIsFiltered(fieldName) {
        return this.defaultGenesColumns.includes(fieldName)
            ? this.genesFilter[fieldName] !== undefined
            : this.genesFilter.additionalFilters[fieldName] !== undefined;
    }

    clearGeneFieldFilter(fieldName) {
        if (this.defaultGenesColumns.includes(fieldName)) {
            this.genesFilter[fieldName] = undefined;
        } else {
            this.genesFilter.additionalFilters[fieldName] = undefined;
        }
        this.dispatcher.emit('genes:refresh');
    }

    clearGenesFilter() {
        if (this._blockFilterGenes) {
            clearTimeout(this._blockFilterGenes);
            this._blockFilterGenes = null;
        }
        this._hasMoreGenes = true;
        this._genesFilter = {
            additionalFilters: {}
        };
        this.dispatcher.emit('genes:refresh');
        this._blockFilterGenes = setTimeout(() => {
            this._blockFilterGenes = null;
        }, blockFilterGenesTimeout);
    }

    canScheduleFilterGenes() {
        return !this._blockFilterGenes;
    }

    scheduleFilterGenes() {
        if (this._blockFilterGenes) {
            return;
        }
        this.dispatcher.emit('genes:refresh');
    }

    resetGenesFilter() {
        if (!this.projectContext.genesFilterIsDefault) {
            this.clearGenesFilter();
        }
    }
}
