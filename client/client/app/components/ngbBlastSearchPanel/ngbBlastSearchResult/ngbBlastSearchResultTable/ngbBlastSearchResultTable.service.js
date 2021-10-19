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
        const headerCells = require('./ngbBlastSearchResultTable_header.tpl.html');

        const result = [];
        const columnsList = this.blastSearchResultColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'queryCover':
                case 'percentIdentity': {
                    result.push({
                        cellFilter: 'percentage:2:this',
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: column
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
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
        // sequenceAccessionVersion can be in <reference:chromosome> or in <chromosome> format
        const splitSequenceId = result.sequenceAccessionVersion.split(':');
        return {
            alignments: result.alignments.map(alignment => {
                const splitAlignmentSequenceId = alignment.sequenceAccessionVersion.split(':');
                return {
                    ...alignment,
                    sequenceAccessionVersion: splitAlignmentSequenceId[1] || result.sequenceAccessionVersion,
                    referenceName: splitAlignmentSequenceId[1] ? splitAlignmentSequenceId[0] : undefined,
                };
            }),
            sequenceId: result.sequenceId,
            sequenceAccessionVersion: splitSequenceId[1] || result.sequenceAccessionVersion,
            referenceName: splitSequenceId[1] ? splitSequenceId[0] : undefined,
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
