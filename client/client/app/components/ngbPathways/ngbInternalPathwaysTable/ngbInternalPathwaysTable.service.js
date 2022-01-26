import ClientPaginationService from '../../../shared/services/clientPaginationService';
import {calculateColor} from '../../../shared/utils/calculateColor';

const DEFAULT_INTERNAL_PATHWAYS_COLUMNS = [
    'name', 'description'
];
const DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS = {
    'name': 'name',
    'description': 'description'
};
const INTERNAL_PATHWAYS_COLUMN_TITLES = {
    name: 'Name',
    description: 'Description'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbInternalPathwaysTableService extends ClientPaginationService {

    _internalPathwaysResult;

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'pathways:internalPathways:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _internalPathways;

    get internalPathways() {
        return this._internalPathways;
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    get columnTitleMap() {
        return INTERNAL_PATHWAYS_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS;
    }

    get internalPathwaysColumns() {
        if (!localStorage.getItem('internalPathwaysColumns')) {
            localStorage.setItem('internalPathwaysColumns', JSON.stringify(DEFAULT_INTERNAL_PATHWAYS_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('internalPathwaysColumns'));
    }

    set internalPathwaysColumns(columns) {
        localStorage.setItem('internalPathwaysColumns', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbInternalPathwaysTableService(dispatcher, genomeDataService);
    }

    getInternalPathwaysResultById(id) {
        return this._internalPathwaysResult[id];
    }

    getInternalPathwaysById(id) {
        return this.internalPathways.filter(h => h.groupId === id)[0] || {};
    }

    async searchInternalPathways(currentSearch) {
        const result = await this.loadInternalPathways(currentSearch);
        this._internalPathways = result.internalPathways;
        this._internalPathwaysResult = result.internalPathwaysResult;
        this.dispatcher.emitSimpleEvent('internalPathways:result:change');
    }

    async loadInternalPathways(currentSearch) {
        const emptyResult = {
            internalPathways: [],
            internalPathwaysResult: {}
        };
        const filter = {
            query: currentSearch,
            page: this.currentPage,
            pageSize: this.pageSize
        };
        const data = await this.genomeDataService.getInternalPathwaysLoad(filter);
        if (data.error) {
            this.totalPages = 0;
            this.currentPage = FIRST_PAGE;
            this._firstPage = FIRST_PAGE;
            this._pageError = data.message;
            return emptyResult;
        } else {
            this._pageError = null;
        }
        this.totalPages = Math.ceil(data.totalCount / this.pageSize);
        if (data && data.items) {
            return {
                internalPathways: this.getInternalPathwaysSearch(data.items),
                internalPathwaysResult: this.getInternalPathwaysResult(data.items)
            };
        } else {
            return emptyResult;
        }
    }

    getInternalPathwaysSearch(data) {
        const result = [];
        data.forEach(value => result.push(this._formatServerToClient(value)));
        return result;
    }

    getInternalPathwaysResult(data) {
        return data;
        let maxHomologLength = 0;
        const result = {};
        if (data) {
            data.forEach(internalPathways => {
                maxHomologLength = 0;
                result[internalPathways.groupId] = [];
                internalPathways.genes.forEach((gene, key) => {
                    result[internalPathways.groupId][key] = this._formatResultToClient(gene);
                    if (maxHomologLength < result[internalPathways.groupId][key].aa) {
                        maxHomologLength = result[internalPathways.groupId][key].aa;
                    }
                });
                result[internalPathways.groupId].forEach((value, key) => {
                    result[internalPathways.groupId][key].domainsObj = {
                        domains: value.domains.map(d => ({...d, color: calculateColor(d.name)})),
                        homologLength: value.aa,
                        maxHomologLength: maxHomologLength,
                        accession_id: value.accession_id
                    };
                    delete result[internalPathways.groupId][key].domains;
                });
            });
        }
        return result;
    }

    getInternalPathwaysGridColumns() {
        const result = [];
        const columnsList = this.internalPathwaysColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_INTERNAL_PATHWAYS_COLUMNS[column] || column);
                const [columnSortingConfiguration] = this.orderBy.filter(o => o.field === fieldName);
                if (columnSortingConfiguration) {
                    sortingPriority = this.orderBy.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            switch (column) {
                case 'name': {
                    columnSettings = {
                        cellTemplate: `<div class="ui-grid-cell-contents pathways-link"
                                       >{{row.entity.name}}</div>`,
                        enableHiding: false,
                        enableColumnMenu: false,
                        field: 'name',
                        name: this.columnTitleMap[column]
                    };
                    break;
                }
                default: {
                    columnSettings = {
                        enableHiding: false,
                        enableColumnMenu: false,
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

    _formatServerToClient(internalPathways) {
        return internalPathways;
        const gene = new Set();
        const proteinFrequency = {};
        internalPathways.genes.forEach(g => {
            gene.add(g.symbol);
            if (proteinFrequency.hasOwnProperty(g.title)) {
                proteinFrequency[g.title] += 1;
            } else {
                proteinFrequency[g.title] = 1;
            }
        });

        const sortableProteinFrequency = [];
        for (const protein in proteinFrequency) {
            if (proteinFrequency.hasOwnProperty(protein)) {
                sortableProteinFrequency.push([protein, proteinFrequency[protein]]);
            }
        }

        sortableProteinFrequency.sort((b, a) => a[1] - b[1]);

        return {
            groupId: internalPathways.groupId,
            gene: [...gene].sort().join(', '),
            protein: sortableProteinFrequency[0] ? sortableProteinFrequency[0][0] : '',
            info: internalPathways.caption
        };
    }

    _formatResultToClient(result) {
        return {
            geneId: result.geneId,
            name: result.symbol,
            species: result.speciesScientificName,
            accession_id: result.protAcc,
            protGi: result.protGi,
            aa: result.protLen,
            taxId: result.taxId,
            protein: result.title,
            domains: (result.domains || []).map(d => ({
                id: d.pssmId,
                start: d.begin,
                end: d.end,
                name: d.cddName
            }))
        };
    }

}
