const DEFAULT_COLUMNS = [
    'id', 'organism', 'taxId', 'maxScore', 'totalScore', 'queryCover',
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
        return DEFAULT_COLUMNS;
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
                case 'id': {
                    result.push({
                        cellTemplate: `<a class="ui-grid-cell-contents blast-search-result-link sequence-link" 
                                        ng-href="{{row.entity.href}}"
                                       >{{row.entity.id}}</a>`,
                        enableHiding: false,
                        field: 'id',
                        minWidth: 40,
                        name: 'id'
                    });
                    break;
                }
                case 'queryCover':
                case 'percentIdentity': {
                    result.push({
                        cellFilter: 'percentage:this:0',
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

    async updateSearchResult() {
        this._blastSearchResult = await this.loadBlastSearchResult();
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
        return data;
    }
}
