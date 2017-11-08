import {camelPad} from '../../../shared/utils/String.js'

const DEFAULT_BLAT_COLUMNS = [
    'chr', 'startIndex', 'endIndex', 'strand', 'score',
    'match', 'misMatch', 'repMatch',
    'ns',
    'qGapCount', 'qGapBases',
    'tGapCount', 'tGapBases',
];

export default class ngbBlatSearchService {

    static instance(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService) {
        return new ngbBlatSearchService(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService);
    }

    _orderBy = null;
    _detailedRead = null;
    _columnsWidth = { 'chr' : 45, 'startIndex' : 110, 'endIndex' : 110, 'strand' : 90, 'score' : 120 };
    bamDataService;

    constructor(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService) {
        Object.assign(this, {genomeDataService, projectDataService, variantsTableMessages, uiGridConstants, bamDataService});
    }

    get blatColumns() {
        if (localStorage.getItem('blatColumns') === null || localStorage.getItem('blatColumns') === undefined) {
            localStorage.setItem('blatColumns', JSON.stringify(DEFAULT_BLAT_COLUMNS));
        }
        let columns = JSON.parse(localStorage.getItem('blatColumns'));
        let defaultColumnsExists = true;
        for (let i = 0; i < DEFAULT_BLAT_COLUMNS.length; i++) {
            if (columns.map(c => c.toLowerCase()).indexOf(DEFAULT_BLAT_COLUMNS[i].toLowerCase()) === -1) {
                defaultColumnsExists = false;
                break;
            }
        }
        if (!defaultColumnsExists) {
            columns = DEFAULT_BLAT_COLUMNS.map(c => c);
            localStorage.setItem('blatColumns', JSON.stringify(columns || []));
        }

        return columns;
    }

    set blatColumns(columns) {
        localStorage.setItem('blatColumns', JSON.stringify(columns || []));
        const oldColumns = this.blatColumns.sort().reduce((names, name) => {
            return `${names}|${name}`;
        }, '');
        const newColumns = columns.sort().reduce((names, name) => {
            return `${names}|${name}`;
        }, '');
        if (newColumns !== oldColumns) {
            this._isBlatInitialized = false;
        }
    }

    get columnsWidth() {
        return this._columnsWidth;
    }

    get orderBy() {
        return this._orderBy;
    }

    set orderBy(orderBy) {
        this._orderBy = orderBy;
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
        const columnsList = this.blatColumns;

        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];

            let sortDirection = 0;
            if(this.orderBy) {
                const currentOrderByFieldVariations = this.orderBy[0].field;
                const currentOrderByDirectionVariations = this.orderBy[0].desc ? 'desc' : 'asc';
                sortDirection = currentOrderByFieldVariations === column ? currentOrderByDirectionVariations : 0;
            }

            result.push({
                enableHiding: false,
                field: column,
                headerCellTemplate: headerCells,
                minWidth: this.columnsWidth[column] ? this.columnsWidth[column] : 50,
                name: camelPad(column),
                width: '*',
                sort: {
                    direction: sortDirection
                },
            });
        }

        return result;
    }
}