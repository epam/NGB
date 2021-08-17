import ClientPaginationService from '../../../shared/services/clientPaginationService';

const DEFAULT_HOMOLOGENE_COLUMNS = [
    'gene', 'protein', 'info'
];
const DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS = {
    'gene': 'gene',
    'protein': 'protein',
    'info': 'info'
};
const HOMOLOGENE_COLUMN_TITLES = {
    gene: 'Gene Name',
    protein: 'Protein Name',
    info: 'Info'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbHomologeneTableService extends ClientPaginationService {

    _homologeneResult;

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'homologs:homologene:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _homologene;

    get homologene() {
        return this._homologene;
    }

    getHomologeneResultById(id) {
        return this._homologeneResult[id];
    }

    getHomologeneById(id) {
        return this.homologene.filter(h => h.groupId === id)[0] || {};
    }

    _pageError = null;


    get pageError() {
        return this._pageError;
    }

    get columnTitleMap() {
        return HOMOLOGENE_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS;
    }

    get homologeneColumns() {
        if (!localStorage.getItem('homologeneColumns')) {
            localStorage.setItem('homologeneColumns', JSON.stringify(DEFAULT_HOMOLOGENE_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('homologeneColumns'));
    }

    set homologeneColumns(columns) {
        localStorage.setItem('homologeneColumns', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbHomologeneTableService(dispatcher, genomeDataService);
    }

    async searchHomologene(currentSearch) {
        const result = await this.loadHomologene(currentSearch);
        this._homologene = result.homologene;
        this._homologeneResult = result.homologeneResult;
        this.dispatcher.emitSimpleEvent('homologene:result:change');
    }

    async loadHomologene(currentSearch) {
        const emptyResult = {
            homologene: [],
            homologeneResult: {}
        };
        const filter = {
            query: currentSearch,
            page: this.currentPage,
            pageSize: this.pageSize
        };
        const data = await this.genomeDataService.getHomologeneLoad(filter);
        if (data.error) {
            this._totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._pageError = data.message;
            return emptyResult;
        } else {
            this._pageError = null;
        }
        this._totalPages = Math.ceil(data.totalCount / this.pageSize);
        if (data && data.items) {
            return {
                homologene: this.getHomologeneSearch(data.items),
                homologeneResult: this.getHomologeneResult(data.items)
            };
        } else {
            return emptyResult;
        }
    }

    getHomologeneSearch(data) {
        const result = [];
        data.forEach(value => result.push(this._formatServerToClient(value)));
        return result;
    }

    getHomologeneResult(data) {
        let maxHomologLength = 0;
        const result = {};
        if (data) {
            data.forEach(homologene => {
                result[homologene.groupId] = [];
                homologene.genes.forEach((gene, key) => {
                    result[homologene.groupId][key] = this._formatResultToClient(gene);
                    if (maxHomologLength < result[homologene.groupId][key].aa) {
                        maxHomologLength = result[homologene.groupId][key].aa;
                    }
                });
                result[homologene.groupId].forEach((value, key) => {
                    result[homologene.groupId][key].domainsObj = {
                        domains: value.domains.map(d => ({...d, color: this.calculateColor(d.name)})),
                        homologLength: value.aa,
                        maxHomologLength: maxHomologLength
                    };
                    delete result[homologene.groupId][key].domains;
                });
            });
        }
        return result;
    }

    getHomologeneGridColumns() {
        const result = [];
        const columnsList = this.homologeneColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_HOMOLOGENE_COLUMNS[column] || column);
                const [columnSortingConfiguration] = this.orderBy.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderBy.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'gene': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link"
                                       >{{row.entity.gene}}</div>`,
                        enableHiding: false,
                        field: 'gene',
                        name: this.columnTitleMap[column]
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        field: column,
                        minWidth: 40,
                        name: this.columnTitleMap[column],
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

    _formatServerToClient(homologene) {
        const gene = new Set(),
            protein = [];
        homologene.genes.forEach(g => {
            gene.add(g.symbol);
            // protein.push(g.title);
        });

        return {
            groupId: homologene.groupId,
            gene: [...gene].sort().join(', '),
            protein: protein.join(', '),
            info: homologene.caption
        };
    }

    _formatResultToClient(result) {
        return {
            geneId: result.geneId,
            name: result.symbol,
            species: result.species,
            accession_id: result.protAcc,
            protGi: result.protGi,
            aa: result.protLen,
            domains: (result.domains || []).map(d => ({
                id: d.pssmId,
                start: d.begin,
                end: d.end,
                name: d.cddName
            }))
        };
    }


    calculateColor(str) {
        return this.intToRGB(this.hashCode(str));
    }

    //TODO: move this and gene's to utility
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

}
