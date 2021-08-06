const DEFAULT_COLUMNS = [
    'name', 'species', 'accession_id', 'aa', 'domains'
];

const COLUMNS_GROUP = {
    name: 'Gene',
    species: 'Gene',
    accession_id: 'Protein',
    aa: 'Protein',
    domains: 'Protein'
};

const HOMOLOGENE_COLUMN_TITLES = {
    'accession_id': 'Accession ID'
};

export default class ngbHomologeneResultService {
    _searchResultTableLoading = true;

    constructor(dispatcher, genomeDataService) {
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _searchResultTableError = null;

    get searchResultTableError() {
        return this._searchResultTableError;
    }

    _homologeneResult = {};

    get homologeneResult() {
        return this._homologeneResult;
    }

    get homologeneResultColumns() {
        if (localStorage.getItem('homologeneResultColumns') === null || localStorage.getItem('homologeneResultColumns') === undefined) {
            localStorage.setItem('homologeneResultColumns', JSON.stringify(DEFAULT_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('homologeneResultColumns'));
    }

    set homologeneResultColumns(columns) {
        localStorage.setItem('homologeneResultColumns', JSON.stringify(columns || []));
    }

    get columnTitleMap() {
        return HOMOLOGENE_COLUMN_TITLES;
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbHomologeneResultService(dispatcher, genomeDataService);
    }

    getHomologeneResultGridColumns() {
        const headerCells = require('./ngbHomologeneResultTable/ngbHomologeneResultTable_header.tpl.html');

        const result = [];
        const columnsList = this.homologeneResultColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'domains': {
                    result.push({
                        cellTemplate: '<ngb-homologs-domains domains="row.entity.domainsObj"></ngb-homologs-domains>',
                        enableHiding: false,
                        field: 'domainsObj',
                        group: COLUMNS_GROUP[column],
                        headerCellTemplate: headerCells,
                        minWidth: 300,
                        name: column
                    });
                    break;
                }
                case 'name': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link"
                                       >{{row.entity.name}}</div>`,
                        enableHiding: false,
                        field: 'name',
                        group: COLUMNS_GROUP[column],
                        headerCellTemplate: headerCells,
                        name: column
                    });
                    break;
                }
                case 'accession_id': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link"
                                       >{{row.entity.accession_id}}</div>`,
                        enableHiding: false,
                        field: 'accession_id',
                        group: COLUMNS_GROUP[column],
                        headerCellTemplate: headerCells,
                        name: column
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        group: COLUMNS_GROUP[column],
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.columnTitleMap[column] || column,
                        width: '*'
                    });
                    break;
                }
            }
        }
        return result;
    }

    async updateSearchResult(searchId) {
        this._homologeneResult = await this.loadHomologeneResult(searchId);
        this.dispatcher.emitSimpleEvent('homologene:result:change');
    }

    async loadHomologeneResult(searchId) {
        const data = await this.genomeDataService.getHomologeneResultLoad(searchId);
        const domainTypes = {};
        let maxHomologLength = 0;
        const colors = {
            'A': 'red',
            'B': 'blue',
            'C': 'green',
            'D': 'yellow',
        };
        if (data.error) {
            this._searchResultTableLoading = false;
            this._searchResultTableError = data.message;
            return [];
        } else {
            this._searchResultTableError = null;
        }
        if (data) {
            data.forEach((value, key) => {
                data[key] = this._formatServerToClient(value);
                if (maxHomologLength < data[key].aa) {
                    maxHomologLength = data[key].aa;
                }
                data[key].domains.forEach(d => {
                    if (!domainTypes[d.name]) {
                        domainTypes[d.name] = {
                            id: d.id,
                            name: d.name,
                            color: colors[d.name]
                        };
                    }
                });
            });
            data.forEach((value, key) => {
                data[key].domainsObj = {
                    domains: data[key].domains.map(d => ({...d, color: colors[d.name]})),
                    homologLength: data[key].aa,
                    maxHomologLength: maxHomologLength
                };
                delete data[key].domains;
            });
        }
        return {
            data: data || [],
            domainTypes: domainTypes || []
        };
    }

    _formatServerToClient(result) {
        return {
            name: result.name,
            species: result.species,
            accession_id: result.accession_id,
            aa: result.aa,
            domains: result.domains
        };
    }
}
