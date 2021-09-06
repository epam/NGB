import ClientPaginationService from '../../../shared/services/clientPaginationService';

const DEFAULT_COLUMNS = [
    'name', 'species', 'aa', 'domains', 'type'
];

const ORTHO_PARA_COLUMN_TITLES = {};

const ORTHO_PARA_TYPE_VIEW = {
    'ORTHOLOG': 'Ortholog',
    'PARALOG': 'Paralog'
};

const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbOrthoParaResultService extends ClientPaginationService {

    constructor(dispatcher, genomeDataService, uiGridConstants) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'homologs:orthoPara:result:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
        this.uiGridConstants = uiGridConstants;
    }

    get orthoParaResultColumns() {
        if (localStorage.getItem('orthoParaResultColumns') === null || localStorage.getItem('orthoParaResultColumns') === undefined) {
            localStorage.setItem('orthoParaResultColumns', JSON.stringify(DEFAULT_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('orthoParaResultColumns'));
    }

    set orthoParaResultColumns(columns) {
        localStorage.setItem('orthoParaResultColumns', JSON.stringify(columns || []));
    }

    get columnTitleMap() {
        return ORTHO_PARA_COLUMN_TITLES;
    }

    get typeViewMap() {
        return ORTHO_PARA_TYPE_VIEW;
    }

    static instance(dispatcher, genomeDataService, uiGridConstants) {
        return new ngbOrthoParaResultService(dispatcher, genomeDataService, uiGridConstants);
    }

    getOrthoParaResultGridColumns() {
        const headerCells = require('./ngbOrthoParaResultTable/ngbOrthoParaResultTable_header.tpl.html');

        const result = [];
        const columnsList = this.orthoParaResultColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'domains': {
                    result.push({
                        cellTemplate: '<ngb-homologs-domains domains="row.entity.domainsObj"></ngb-homologs-domains>',
                        enableHiding: false,
                        enableSorting: false,
                        enableColumnMenu: false,
                        field: 'domainsObj',
                        headerCellTemplate: headerCells,
                        minWidth: 300,
                        name: column
                    });
                    break;
                }
                case 'name': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link" 
                                            ng-click="grid.appScope.$ctrl.navigateToTrack(row.entity)">
                                            {{row.entity.name || 'id: ' + row.entity.geneId}}
                                       </div>`,
                        enableHiding: false,
                        field: 'name',
                        headerCellTemplate: headerCells,
                        name: column,
                        sortingAlgorithm: this.sortName
                    });
                    break;
                }
                case 'type': {
                    result.push({
                        cellTemplate: `<div class="ui-grid-cell-contents">
                                            {{grid.appScope.$ctrl.typeViewMap[row.entity.type]}}
                                       </div>`,
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: this.columnTitleMap[column] || column,
                        width: '*'
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
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

    sortName = (a, b, rowA, rowB)  => {
        if (a) {
            if (b) {
                return a > b ? 1 : a < b ? -1 : 0;
            } else {
                return -1;
            }
        } else if (b) {
            return 1;
        } else {
            return rowA.entity.geneId - rowB.entity.geneId;
        }
    }
}
