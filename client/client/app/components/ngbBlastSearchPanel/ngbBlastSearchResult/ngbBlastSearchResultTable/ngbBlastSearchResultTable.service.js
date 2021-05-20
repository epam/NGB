const DEFAULT_COLUMNS = [
    'id', 'organism', 'taxId', 'maxScore', 'totalScore', 'queryCover',
    'eValue', 'percentIdentity', 'matches'
];

export default class ngbBlastSearchResultTableService {
    static instance() {
        return new ngbBlastSearchResultTableService();
    }

    _searchResultTableError = null;
    _searchResultTableLoading = true;
    _blastSearchResult;

    constructor() {
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
                        cellTemplate: `<a class="ui-grid-cell-contents sequence-link" 
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
    
    async loadBlastSearchResult() {
        let data = this._getRandomSearchResult(30);
        if (data.error) {
            this._searchResultTableLoading = false;
            this._searchResultTableError = data.message;
            return [];
        } else {
            this._searchResultTableError = null;
        }        
        return data;
    }

    // TODO: remove before merge;
    _getRandomSearchResult(length) {
        const result = [];
        for (let i = 0; i < length; i++) {
            result.push({
                id: i + 1,
                organism: [1, 2, 3][Math.floor(Math.random() * 2)],
                taxId: 2*i+1,
                maxScore: Math.round(Math.random()*90) + 10,
                totalScore: Math.round(Math.random()*210) + 90,
                queryCover: Math.random().toFixed(2),
                eValue: Math.round(Math.random()*190) + 10,
                percentIdentity: Math.random().toFixed(2),
                matches: Math.round(Math.random()*5) + 1
            });
        }
        return result;
    }
}
