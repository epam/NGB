import ClientPaginationService from '../../../shared/services/clientPaginationService';

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

const FIRST_PAGE = 1;
const PAGE_SIZE = 15;

export default class ngbHomologeneResultService extends ClientPaginationService {

    constructor(dispatcher, genomeDataService) {
        super(dispatcher, FIRST_PAGE, PAGE_SIZE, 'homologs:homologene:result:page:change');
        this.dispatcher = dispatcher;
        this.genomeDataService = genomeDataService;
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
                        enableSorting: false,
                        enableColumnMenu: false,
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
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link">
                                        <a target="_blank" ng-href="https://www.ncbi.nlm.nih.gov/gene/{{row.entity.geneId}}">
                                            {{row.entity.name}}
                                        </a>
                                       </div>`,
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
                        cellTemplate: `<div class="ui-grid-cell-contents homologs-link">
                                        <a target="_blank" ng-href="https://www.ncbi.nlm.nih.gov/protein/{{row.entity.accession_id}}">
                                            {{row.entity.accession_id}}
                                        </a>
                                       </div>`,
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
}
