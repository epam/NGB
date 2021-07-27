const DEFAULT_GENES_COLUMNS = [
    'chromosome', 'featureName', 'featureId', 'featureType', 'startIndex', 'endIndex', 'strand'
];
const SERVICE_GENES_COLUMNS = ['info'];
const OPTIONAL_GENE_COLUMNS = [
    'featureFileId', 'source', 'score', 'frame'
];
const DEFAULT_ORDERBY_GENES_COLUMNS = {
    'chromosome': 'CHROMOSOME_NAME',
    'featureName': 'FEATURE_NAME',
    'featureId': 'FEATURE_ID',
    'featureType': 'FEATURE_TYPE',
    'startIndex': 'START_INDEX',
    'endIndex': 'END_INDEX',
    'strand': 'strand'
};

const SERVER_COLUMN_NAMES = {
};
const GENES_COLUMN_TITLES = {
    chromosome: 'Chr',
    featureName: 'Name',
    featureId: 'Id',
    source: 'source',
    featureType: 'Type',
    startIndex: 'Start',
    endIndex: 'End',
    strand: 'Strand',
    info: 'Info',
    molecularView: 'Molecular View'
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
        return DEFAULT_GENES_COLUMNS.concat(SERVICE_GENES_COLUMNS);
    }

    get genesTableColumns() {
        if (!localStorage.getItem('genesTableColumns')) {
            localStorage.setItem('genesTableColumns', JSON.stringify(this.defaultGenesColumns));
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
            this._optionalGenesColumns = OPTIONAL_GENE_COLUMNS.concat(data.availableFilters);
            this.genesTableColumns = DEFAULT_GENES_COLUMNS
                .concat(this.genesTableColumns.filter(c => this._optionalGenesColumns.includes(c)))
                .concat(SERVICE_GENES_COLUMNS);
            this.dispatcher.emit('genes:info:loaded');
        });
        this.genomeDataService.filterGeneValues(this.projectContext.reference.id, 'featureType').then(data => {
            this._geneTypeList = data.map(type => ({
                label: type.toUpperCase(),
                value: type.toUpperCase()
            }));
            this.dispatcher.emit('genes:values:loaded');
        });
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
            chromosomeIds: this.genesFilter.chromosome || [],
            startIndex: this.genesFilter.startIndex,
            endIndex: this.genesFilter.endIndex,
            featureNames: this.genesFilter.featureName || [],
            featureId: this.genesFilter.featureId,
            featureTypes: this.genesFilter.featureType || [],
            additionalFilters: this.genesFilter.additionalFilters || {},
            attributesFields: this.genesTableColumns.filter(c => !this.defaultGenesColumns.includes(c)),
            pageSize: this.genesPageSize,
            pointer: isScrollTop ? this.prevPagePointer : this.nextPagePointer,
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
            switch (this.genesFilter.strand) {
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
            .filter(column => column !== 'info')
            .map(column => SERVER_COLUMN_NAMES[column] || column);
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
                        field: '',
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
                case 'featureType': {
                    columnSettings = {
                        cellTemplate: `<div class="md-label variation-type"
                                    ng-style="grid.appScope.$ctrl.getStyle(COL_FIELD)"
                                    ng-class="COL_FIELD CUSTOM_FILTERS" >{{row.entity.feature}}</div>`,
                        enableHiding: false,
                        field: 'featureType',
                        filter: {
                            selectOptions: this.geneTypeList,
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
            chromosome: search.chromosome ? search.chromosome.name : undefined,
            chromosomeObj: search.chromosome
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
