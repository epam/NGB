const DEFAULT_COLUMNS = [
    'sequenceId', 'organism', 'taxId', 'maxScore', 'totalScore', 'queryCover',
    'eValue', 'percentIdentity', 'matches'
];

export default class ngbBlastSearchResultTableService {
    static instance(projectDataService) {
        return new ngbBlastSearchResultTableService(projectDataService);
    }

    _searchResultTableError = null;
    _searchResultTableLoading = true;
    _blastSearchResult;

    constructor(projectDataService) {
        this.projectDataService = projectDataService;
    }

    get blastSearchResultColumns() {
        if (localStorage.getItem('blastSearchResultColumns') === null || localStorage.getItem('blastSearchResultColumns') === undefined) {
            localStorage.setItem('blastSearchResultColumns', JSON.stringify(DEFAULT_COLUMNS));
        }
        return JSON.parse(localStorage.getItem('blastSearchResultColumns'));
    }

    set blastSearchResultColumns(columns) {
        localStorage.setItem('blastSearchResultColumns', JSON.stringify(columns || []));
    }

    get searchResultTableError() {
        return this._searchResultTableError;
    }

    get blastSearchResult() {
        return this._blastSearchResult;
    }

    getBlastSearchResultGridColumns() {
        const result = [];
        const columnsList = this.blastSearchResultColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'sequenceId': {
                    result.push({
                        cellTemplate: `<a class="ui-grid-cell-contents blast-search-result-link sequence-link" 
                                        ng-href="{{row.entity.href}}"
                                       >{{row.entity.sequenceId}}</a>`,
                        enableHiding: false,
                        field: 'sequenceId',
                        minWidth: 40,
                        name: 'sequenceId'
                    });
                    break;
                }
                case 'queryCover':
                case 'percentIdentity': {
                    result.push({
                        cellFilter: 'percentage:2:this',
                        enableHiding: false,
                        field: column,
                        minWidth: 40,
                        name: column
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        minWidth: 40,
                        name: column,
                        width: '*'
                    });
                    break;
                }
            }
        }
        return result;
    }

    async updateSearchResult(searchId) {
        this._blastSearchResult = await this.loadBlastSearchResult(searchId);
    }

    async loadBlastSearchResult(searchId) {
        const data = await this.projectDataService.getBlastResultLoad(searchId);
        if (data.error) {
            this._searchResultTableLoading = false;
            this._searchResultTableError = data.message;
            return [];
        } else {
            this._searchResultTableError = null;
        }
        if (data) {
            data.forEach((value, key) => data[key] = this._formatServerToClient(value));
        }
        return data || [];
    }

    _formatServerToClient(result) {
        return {
            alignments: result.alignments,
            sequenceId: result.sequenceId,
            organism: result.organism,
            taxId: result.taxId,
            maxScore: result.maxScore,
            totalScore: result.totalScore,
            queryCover: result.queryCoverage / 100,
            eValue: result.evalue,
            percentIdentity: result.percentIdentity / 100,
            matches: result.matches
        };
    }
}
