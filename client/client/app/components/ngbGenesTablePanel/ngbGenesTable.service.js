const DEFAULT_GENES_COLUMNS = [
    'chr', 'gene', 'gene_id', 'start', 'end', 'strand', 'info', 'molecularView'
];
const ALL_GENES_COLUMNS = [
    'chr', 'gene', 'gene_id', 'start', 'end', 'strand', 'gene_source', 'info', 'molecularView'
];
const DEFAULT_ORDERBY_GENES_COLUMNS = {
    'chr': 'CHROMOSOME_NAME',
    'gene': 'FEATURE_NAME',
    'gene_id': 'FEATURE_ID',
    'start': 'START_INDEX',
    'end': 'END_INDEX'
};
const GENES_COLUMN_TITLES = {
    chr: 'Chr',
    gene: 'Gene',
    gene_id: 'Gene Id',
    gene_source: 'Gene Source',
    start: 'Start',
    end: 'End',
    strand: 'Strand',
    info: 'Info',
    molecularView: 'Molecular View'
};
const PAGE_SIZE = 100;
const blockFilterGenesTimeout = 500;

export default class ngbGenesTableService {

    _hasMoreGenes = true;
    _blockFilterGenes;

    constructor(dispatcher, projectDataService, genomeDataService, projectContext) {
        this.dispatcher = dispatcher;
        this.projectDataService = projectDataService;
        this.genomeDataService = genomeDataService;
        this.projectContext = projectContext;
        this.initEvents();
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

    _genesFilterIsDefault = true;

    get genesFilterIsDefault() {
        return this._genesFilterIsDefault;
    }

    _displayGenesFilter = {};

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
        if (!localStorage.getItem('genesTableColumns')) {
            localStorage.setItem('genesTableColumns', JSON.stringify(DEFAULT_GENES_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('genesTableColumns'));
    }

    set genesTableColumns(columns) {
        localStorage.setItem('genesTableColumns', JSON.stringify(columns || []));
    }

    get allGenesColumns() {
        return ALL_GENES_COLUMNS;
    }

    get optionalGenesColumns() {
        return this.allGenesColumns
            .filter(c => !~this.defaultGenesColumns.indexOf(c));
    }

    static instance(dispatcher, projectDataService, genomeDataService, projectContext) {
        return new ngbGenesTableService(dispatcher, projectDataService, genomeDataService, projectContext);
    }

    initEvents() {
        this.dispatcher.on('genes:reset:filter', ::this.resetGenesFilter);
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
            // chromosome: this.genesFilter.chromosome,
            // strand: this.genesFilter.strand,
            // start: this.genesFilter.start,
            // end: this.genesFilter.end,
            // gene: this.genesFilter.gene,
            // additionalFilters: this.genesFilter.additionalFilters || {},
            pageSize: this.genesPageSize,
            pointer: page,
            orderBy: (this.orderByGenes || []).map(config => ({
                field: config.field,
                desc: !config.ascending
            }))
        };
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
                const fieldName = (DEFAULT_ORDERBY_GENES_COLUMNS[column] || column);
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
                default: {
                    columnSettings = {
                        enableHiding: !this.defaultGenesColumns.includes(column),
                        enableFiltering: true,
                        enableSorting: DEFAULT_ORDERBY_GENES_COLUMNS.hasOwnProperty(column),
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.genesColumnTitleMap[column],
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
        return {
            ...search,
            chr: search.chromosome ? search.chromosome.name : undefined,
            gene: search.featureName,
            gene_id: search.featureId,
            start: search.startIndex,
            end: search.endIndex
        };
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
