const DEFAULT_GENES_COLUMNS = [
    'chromosome', 'featureName', 'featureId', 'featureType', 'startIndex', 'endIndex', 'strand'
];
const SERVICE_GENES_COLUMNS = ['info'];
const OPTIONAL_GENE_COLUMNS = [
    'featureFileId', 'source', 'score', 'frame'
];
const DEFAULT_PREFIX = 'ngb_default_';
const DEFAULT_ORDERBY_GENES_COLUMNS = {
    [`${DEFAULT_PREFIX}chromosome`]: 'CHROMOSOME_NAME',
    [`${DEFAULT_PREFIX}featureName`]: 'FEATURE_NAME',
    [`${DEFAULT_PREFIX}featureId`]: 'FEATURE_ID',
    [`${DEFAULT_PREFIX}featureType`]: 'FEATURE_TYPE',
    [`${DEFAULT_PREFIX}startIndex`]: 'START_INDEX',
    [`${DEFAULT_PREFIX}endIndex`]: 'END_INDEX',
    [`${DEFAULT_PREFIX}strand`]: 'STRAND',
    [`${DEFAULT_PREFIX}featureFileId`]: 'FEATURE_FILE_ID',
    [`${DEFAULT_PREFIX}source`]: 'SOURCE',
    [`${DEFAULT_PREFIX}score`]: 'SCORE',
    [`${DEFAULT_PREFIX}frame`]: 'FRAME',
};

const SERVER_COLUMN_NAMES = {};
const GENES_COLUMN_TITLES = {
    [`${DEFAULT_PREFIX}chromosome`]: 'Chr',
    [`${DEFAULT_PREFIX}featureName`]: 'Name',
    [`${DEFAULT_PREFIX}featureId`]: 'Id',
    [`${DEFAULT_PREFIX}featureType`]: 'Type',
    [`${DEFAULT_PREFIX}startIndex`]: 'Start',
    [`${DEFAULT_PREFIX}endIndex`]: 'End',
    [`${DEFAULT_PREFIX}strand`]: 'Strand',
    [`${DEFAULT_PREFIX}info`]: 'Info',
    [`${DEFAULT_PREFIX}featureFileId`]: 'FeatureFileId',
    [`${DEFAULT_PREFIX}source`]: 'Source',
    [`${DEFAULT_PREFIX}score`]: 'Score',
    [`${DEFAULT_PREFIX}frame`]: 'Frame'
};
const PAGE_SIZE = 100;
const MAX_VISIBLE_PAGES = 3;
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

    _pageList = [];

    get nextPagePointer() {
        return this._pageList[this.lastPage];
    }

    get prevPagePointer() {
        return this._pageList[this.firstPage - 2];
    }

    get totalPages() {
        return this._pageList.length;
    }

    get maxVisiblePages() {
        return MAX_VISIBLE_PAGES;
    }

    _firstPage = 0;

    get firstPage() {
        return this._firstPage;
    }

    set firstPage(value) {
        this._firstPage = value;
    }

    _lastPage = -1; // because initial loading is the same method as scrollDown

    get lastPage() {
        return this._lastPage;
    }

    set lastPage(value) {
        this._lastPage = value;
    }

    _lastPageLength = 0;

    get lastPageLength() {
        return this._lastPageLength;
    }

    getPage(page) {
        return this._pageList[page];
    }

    _geneTypeList = [];
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
        return DEFAULT_GENES_COLUMNS.concat(SERVICE_GENES_COLUMNS);
    }

    get prefixedDefaultGenesColumns() {
        return this.prefixColumns(this.defaultGenesColumns);
    }

    get nonAttributeColumns() {
        return this.defaultGenesColumns.concat(OPTIONAL_GENE_COLUMNS);
    }

    get prefixedNonAttributeColumns() {
        return this.prefixColumns(this.nonAttributeColumns);
    }

    get genesTableColumns() {
        if (!localStorage.getItem('genesTableColumns') || localStorage.getItem('genesTableColumns') === '[]') {
            localStorage.setItem('genesTableColumns', JSON.stringify(this.prefixedDefaultGenesColumns));
        }
        return JSON.parse(localStorage.getItem('genesTableColumns'));
    }

    get defaultPrefix() {
        return DEFAULT_PREFIX;
    }

    getColumnOriginalName(column) {
        return column.startsWith(this.defaultPrefix) ? column.substring(this.defaultPrefix.length) : column;
    }

    prefixColumns(columns = []) {
        const result = [];
        columns.forEach(c => result.push(this.nonAttributeColumns.includes(c) ? this.defaultPrefix + c : c));
        return result;
    }

    set genesTableColumns(columns) {
        localStorage.setItem('genesTableColumns', JSON.stringify(columns || []));
    }

    _optionalGenesColumns = [];

    get optionalGenesColumns() {
        return this._optionalGenesColumns;
    }

    get geneTypeList() {
        return this._geneTypeList;
    }

    resetPagination() {
        this._pageList = [];
        this._firstPage = 0;
        this._lastPage = -1;
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
            this._optionalGenesColumns = this.prefixColumns(OPTIONAL_GENE_COLUMNS).concat(data.availableFilters);
            this.genesTableColumns = this.prefixColumns(DEFAULT_GENES_COLUMNS)
                .concat(this.genesTableColumns.filter(c => this._optionalGenesColumns.includes(c)))
                .concat(this.prefixColumns(SERVICE_GENES_COLUMNS));
            this.dispatcher.emit('genes:info:loaded');
        });
        this.genomeDataService.filterGeneValues(this.projectContext.reference.id, 'featureType').then(data => {
            this._geneTypeList = data.map(type => ({
                label: type.toUpperCase(),
                value: type.toUpperCase()
            }));
            this.dispatcher.emit('genes:values:loaded');
        });
        this.orderByGenes = null;
        this.clearGenesFilter();
    }

    setDisplayGenesFilter(value, updateScope = true) {
        if (value !== this._displayGenesFilter) {
            this._displayGenesFilter = value;
            localStorage.setItem('displayGenesFilter', JSON.stringify(value));
            this.dispatcher.emitSimpleEvent('display:genes:filter', updateScope);
        }
    }

    getRequestFilter(isScrollTop) {
        const filter = {
            chromosomeIds: this.genesFilter[`${this.defaultPrefix}chromosome`] || [],
            startIndex: this.genesFilter[`${this.defaultPrefix}startIndex`],
            endIndex: this.genesFilter[`${this.defaultPrefix}endIndex`],
            featureNames: this.genesFilter[`${this.defaultPrefix}featureName`] || [],
            featureId: this.genesFilter[`${this.defaultPrefix}featureId`],
            featureTypes: this.genesFilter[`${this.defaultPrefix}featureType`] || [],
            featureFileId: this.genesFilter[`${this.defaultPrefix}featureFileId`],
            additionalFilters: this.genesFilter.additionalFilters || {},
            attributesFields: this.genesTableColumns.filter(c => !this.prefixedNonAttributeColumns.includes(c)),
            pageSize: this.genesPageSize,
            pointer: isScrollTop ? this.prevPagePointer : this.nextPagePointer,
            orderBy: (this.orderByGenes || []).map(config => ({
                field: config.field,
                desc: !config.ascending
            }))
        };
        if (this.genesFilter[`${this.defaultPrefix}frame`]) {
            filter.frames = [this.genesFilter[`${this.defaultPrefix}frame`]];
        }
        if (this.genesFilter[`${this.defaultPrefix}source`]) {
            filter.sources = [this.genesFilter[`${this.defaultPrefix}source`]];
        }
        if (this.genesFilter[`${this.defaultPrefix}strand`]) {
            switch (this.genesFilter[`${this.defaultPrefix}strand`]) {
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
        if (this.genesFilter[`${this.defaultPrefix}score`]) {
            filter.score = {
                left: this.genesFilter[`${this.defaultPrefix}score`][0],
                right: this.genesFilter[`${this.defaultPrefix}score`][1]
            };
        }
        const tracks = (this.projectContext.tracks || []).filter(track => track.format === 'GENE');
        if (tracks.length) {
            filter.geneFileIdsByProject = {};
            tracks.forEach(track => {
                let datasetId = track.project ? track.project.id.toString() : undefined;
                if (!datasetId && track.projectId) {
                    if (Number.isNaN(Number(track.projectId))) {
                        const [dataset] = (this.projectContext.datasets || [])
                            .filter(d => d.name === track.projectId);
                        if (dataset) {
                            datasetId = dataset.id;
                        }
                    } else {
                        datasetId = track.projectId;
                    }
                }
                if (datasetId) {
                    if (!filter.geneFileIdsByProject[datasetId]) {
                        filter.geneFileIdsByProject[datasetId] = [];
                    }
                    filter.geneFileIdsByProject[datasetId].push(track.id);
                }
            });
        }
        return filter;
    }

    async loadGenes(reference, isScrollTop) {
        const filter = this.getRequestFilter(isScrollTop);
        this.refreshGenesFilterEmptyStatus();
        try {
            const data = await this.genomeDataService.loadGenes(
                reference,
                filter
            );
            this._genesTableError = null;
            this._hasMoreGenes = !!data.pointer;
            if (isScrollTop) {
                this._pageList.pop();
            } else if (data.pointer) {
                this._pageList.push(data.pointer);
            }
            this._lastPageLength = (data.entries || []).length;
            this.nextPageMarker = data.pointer;
            return (data.entries || [])
                .map(this._formatServerToClient.bind(this))
                .map(feature => ({...feature, referenceId: reference}));
        } catch (e) {
            this._hasMoreGenes = false;
            this._genesTableError = e.message;
            return [];
        }
    }

    downloadFile(reference, format, includeHeader) {
        const exportFields = this.genesTableColumns
            .filter(column => !this.prefixColumns(SERVICE_GENES_COLUMNS).includes(column))
            .map(column => {
                const c = this.getColumnOriginalName(column);
                return SERVER_COLUMN_NAMES[c] || c;
            });
        const filter = this.getRequestFilter(false);
        delete filter.pointer;
        return this.genomeDataService.downloadGenes(
            reference,
            {
                format: format,
                includeHeader: includeHeader
            },
            {
                exportFields: exportFields,
                ...filter
            }
        );
    }

    getGenesGridColumns() {
        const infoCell = require('./ngbGenesTable_info.tpl.html');
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
                case `${this.defaultPrefix}info`: {
                    columnSettings = {
                        cellTemplate: infoCell,
                        enableSorting: false,
                        enableHiding: false,
                        enableFiltering: false,
                        enableColumnMenu: false,
                        field: '',
                        maxWidth: 70,
                        minWidth: 60,
                        name: this.genesColumnTitleMap[column]
                    };
                    break;
                }
                case `${this.defaultPrefix}featureType`: {
                    columnSettings = {
                        cellTemplate: `<div class="md-label variation-type"
                                    ng-style="grid.appScope.$ctrl.getStyle(COL_FIELD)"
                                    ng-class="COL_FIELD CUSTOM_FILTERS" >{{row.entity.feature}}</div>`,
                        enableHiding: false,
                        field: `${this.defaultPrefix}featureType`,
                        filter: {
                            selectOptions: this.geneTypeList,
                            term: '',
                            type: this.uiGridConstants.filter.SELECT
                        },
                        headerCellTemplate: headerCells,
                        headerTooltip: 'Type',
                        maxWidth: 104,
                        minWidth: 104,
                        displayName: 'Type',
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
                    const displayName = this.getColumnDisplayName(column);
                    columnSettings = {
                        enableHiding: !this.prefixedDefaultGenesColumns.includes(column),
                        enableFiltering: true,
                        enableSorting: true,
                        field: column,
                        headerCellTemplate: headerCells,
                        headerTooltip: displayName,
                        minWidth: 40,
                        displayName: displayName,
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

    getColumnDisplayName(column) {
        let result = this.genesColumnTitleMap[column] || column;
        if (!this.prefixedNonAttributeColumns.includes(column)) {
            result += ' (attr)';
        }
        return result;
    }

    _formatServerToClient(search) {
        const result = {};
        for (const key in search) {
            if (search.hasOwnProperty(key)) {
                result[this.nonAttributeColumns.includes(key) ? this.defaultPrefix + key : key] = search[key];
            }
        }
        delete result.attributes;
        result[`${this.defaultPrefix}chromosome`] = search.chromosome ? search.chromosome.name : undefined;
        return {
            ...result,
            ...search.attributes,
            chromosomeObj: search.chromosome
        };
    }

    refreshGenesFilterEmptyStatus() {
        const {additionalFilters, ...defaultFilters} = this.genesFilter;
        const additionalFiltersAreEmpty = Object.entries(additionalFilters).every(field => {
            if (typeof field[1] === 'object') {
                return !Object.keys(field[1]).length;
            } else {
                return field[1] === undefined;
            }
        });
        const defaultFiltersAreEmpty = Object.entries(defaultFilters).every(field => {
            if (typeof field[1] === 'object') {
                return !Object.keys(field[1]).length;
            } else {
                return field[1] === undefined;
            }
        });
        this.projectContext.genesFilterIsDefault = additionalFiltersAreEmpty && defaultFiltersAreEmpty;
    }

    genesFieldIsFiltered(fieldName) {
        return this.prefixedDefaultGenesColumns.includes(fieldName)
            ? this.genesFilter[fieldName] !== undefined
            : this.genesFilter.additionalFilters[fieldName] !== undefined;
    }

    clearGeneFieldFilter(fieldName) {
        if (this.prefixedDefaultGenesColumns.includes(fieldName)) {
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

    hashCode(str) {
        let hash = 0;
        for (let i = 0; i < str.length; i++) {
            hash = str.charCodeAt(i) + ((hash << 5) - hash);
        }
        return 100 * hash;
    }

    intToRGB(i) {
        const c = (i & 0x00FFFFFF)
            .toString(16)
            .toUpperCase();

        return `#${'00000'.substring(0, 6 - c.length)}${c}`;
    }

    determineDarkness(color) {
        const parsedColor = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(color);

        const r = parseInt(parsedColor[1], 16);
        const g = parseInt(parsedColor[2], 16);
        const b = parseInt(parsedColor[3], 16);

        // HSP (Highly Sensitive Poo) equation from http://alienryderflex.com/hsp.html
        const hsp = Math.sqrt(
            0.299 * (r * r) +
            0.587 * (g * g) +
            0.114 * (b * b)
        );

        // Using the HSP value, determine whether the color is light or dark
        return hsp > 127.5 ? '#000' : '#fff';
    }

    getStyle(context) {
        return str => {
            const color = context.intToRGB(context.hashCode(str));
            return {
                'background-color': color,
                'color': context.determineDarkness(color)
            };
        };
    }
}
