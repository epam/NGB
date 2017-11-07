import {camelPad} from '../../../shared/utils/String.js'

export default class ngbBlatSearchService {

    static instance(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService) {
        return new ngbBlatSearchService(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService);
    }

    columnTypes = {
        flag: 'Flag',
        integer: 'Integer',
        string: 'String'
    };

    columnsList = [
        'chr', 'startIndex', 'endIndex', 'strand', 'score',
        'match', 'misMatch', 'repMatch',
        'ns',
        'qGapCount', 'qGapBases',
        'tGapCount', 'tGapBases',
    ];

    columnsWidth = {
        'chr' : 45,
        'startIndex' : 110,
        'endIndex' : 110,
        'strand' : 90,
        'score' : 120,
    };

    _detailedRead = null;
    bamDataService;

    constructor(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService) {
        Object.assign(this, {genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService});
    }

    get readSequence() {
        return this._detailedRead && this._detailedRead.sequence ? this._detailedRead.sequence : null;
    }

    get blatRequest() {
        return JSON.parse(localStorage.getItem('blatSearchRequest')) || null;
    }

    async getDetailedRead(payload) {
        let read = await this.bamDataService.loadRead(payload);

        this._detailedRead = read ? read : null;

        return this._detailedRead;
    }

    async getBlatSearchResults(){
        const payload = this.blatRequest;

        if(!payload) return;

        let searchResults, read;

        read = await this.getDetailedRead(payload);

        searchResults = await this.bamDataService.getBlatSearchResults(payload.id, read.sequence);

        return searchResults;
    }

    getBlatSearchGridColumns() {

        const headerCells = require('./ngbBlatSearch_header.tpl.html');

        const result = [];

        for (let i = 0; i < this.columnsList.length; i++) {
            const column = this.columnsList[i];
            result.push({
                enableHiding: false,
                field: column,
                headerCellTemplate: headerCells,
                minWidth: this.columnsWidth[column] ? this.columnsWidth[column] : 50,
                name: camelPad(column),
                width: '*',
            });
        }

        return result;
    }
}