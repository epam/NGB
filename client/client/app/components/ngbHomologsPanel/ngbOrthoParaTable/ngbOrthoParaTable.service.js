import ClientPaginationService from '../../../shared/services/clientPaginationService';
import {calculateColor} from '../../../shared/utils/calculateColor';

const DEFAULT_ORTHO_PARA_COLUMNS = [
    'gene', 'protein', 'info'
];
const DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS = {
    'gene': 'gene',
    'protein': 'protein',
    'info': 'info'
};
const ORTHO_PARA_COLUMN_TITLES = {
    gene: 'Gene Name',
    protein: 'Protein Name',
    info: 'Info'
};
const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbOrthoParaTableService extends ClientPaginationService {

    _orthoParaResult;

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'homologs:orthoPara:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
    }

    _orthoPara;

    get orthoPara() {
        return this._orthoPara;
    }

    getOrthoParaResultById(id) {
        return this._orthoParaResult[id];
    }

    getOrthoParaById(id) {
        return this.orthoPara.filter(h => h.groupId === id)[0] || {};
    }

    _pageError = null;

    get pageError() {
        return this._pageError;
    }

    get columnTitleMap() {
        return ORTHO_PARA_COLUMN_TITLES;
    }

    get orderByColumns() {
        return DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS;
    }

    get orthoParaColumns() {
        if (!localStorage.getItem('orthoParaColumns')) {
            localStorage.setItem('orthoParaColumns', JSON.stringify(DEFAULT_ORTHO_PARA_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('orthoParaColumns'));
    }

    set orthoParaColumns(columns) {
        localStorage.setItem('orthoParaColumns', JSON.stringify(columns || []));
    }

    static instance(dispatcher, genomeDataService) {
        return new ngbOrthoParaTableService(dispatcher, genomeDataService);
    }

    async searchOrthoPara(currentSearch) {
        const result = await this.loadOrthoPara(currentSearch);
        this._orthoPara = result.orthoPara;
        this._orthoParaResult = result.orthoParaResult;
        this.dispatcher.emitSimpleEvent('orthoPara:result:change');
    }

    async loadOrthoPara(currentSearch) {
        const emptyResult = {
            orthoPara: [],
            orthoParaResult: {}
        };
        const filter = {
            geneId: currentSearch,
            page: this.currentPage,
            pageSize: this.pageSize
        };
        const data = await this.genomeDataService.getOrthoParaLoad(filter);
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
                orthoPara: this.getOrthoParaSearch(data.items),
                orthoParaResult: this.getOrthoParaResult(data.items)
            };
        } else {
            return emptyResult;
        }
    }

    getOrthoParaSearch(data) {
        const result = [];
        data.forEach(value => result.push(this._formatServerToClient(value)));
        return result;
    }

    getOrthoParaResult(data) {
        let maxHomologLength = 0;
        const result = {};
        if (data) {
            data.forEach(orthoPara => {
                maxHomologLength = 0;
                result[orthoPara.groupId] = [];
                orthoPara.homologs.forEach((gene, key) => {
                    result[orthoPara.groupId][key] = this._formatResultToClient(gene, orthoPara.type);
                    if (maxHomologLength < result[orthoPara.groupId][key].aa) {
                        maxHomologLength = result[orthoPara.groupId][key].aa;
                    }
                });
                result[orthoPara.groupId].forEach((value, key) => {
                    result[orthoPara.groupId][key].domainsObj = {
                        domains: value.domains.map(d => ({...d, color: calculateColor(d.name)})),
                        homologLength: value.aa,
                        maxHomologLength: maxHomologLength,
                        accession_id: value.accession_id
                    };
                    delete result[orthoPara.groupId][key].domains;
                });
            });
        }
        return result;
    }

    getOrthoParaGridColumns() {
        const result = [];
        const columnsList = this.orthoParaColumns;
        for (let i = 0; i < columnsList.length; i++) {
            let sortDirection = 0;
            let sortingPriority = 0;
            let columnSettings = null;
            const column = columnsList[i];
            if (this.orderBy) {
                const fieldName = (DEFAULT_ORDERBY_ORTHO_PARA_COLUMNS[column] || column);
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
                        enableColumnMenu: false,
                        field: 'gene',
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

    _formatServerToClient(orthoPara) {
        const targetGenes = orthoPara.homologs.map(h => ({
            geneName: h.symbol,
            geneId: h.ensemblId,
            taxId: h.taxId,
            speciesName: h.speciesScientificName
        }));
        return {
            groupId: orthoPara.groupId,
            gene: orthoPara.geneName,
            protein: orthoPara.proteinName,
            info: orthoPara.homologDatabase,
            targetInfo: {
                targetName: orthoPara.geneName,
                genes: targetGenes
            }
        };
    }

    _formatResultToClient(result, type) {
        return {
            geneId: result.geneId,
            name: result.symbol,
            species: result.speciesScientificName,
            type: type,
            protGi: result.protGi,
            accession_id: result.protAcc,
            aa: result.protLen,
            taxId: result.taxId,
            domains: (result.domains || []).map(d => ({
                id: d.pssmId,
                start: d.begin,
                end: d.end,
                name: d.cddName
            }))
        };
    }
}
