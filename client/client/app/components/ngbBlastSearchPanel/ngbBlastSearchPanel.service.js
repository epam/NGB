import {camelPad} from '../../shared/utils/String.js';

const DEFAULT_BLAST_COLUMNS = [
    'numb', 'chr', 'startIndex', 'endIndex', 'strand', 'score',
    'match', 'mismatch', 'repMatch',
    'ns',
    'qGapCount', 'qGapBases',
    'tGapCount', 'tGapBases',
];

const blastSearchState = {
    DONE: 'DONE',
    FAILURE: 'FAILURE',
    SEARCHING: 'SEARCHING'
};
const FIRST_PAGE = 1;
const PAGE_SIZE_HISTORY = 25;

export default class ngbBlastSearchService {
    static instance(dispatcher, projectContext, bamDataService, uiGridConstants) {
        return new ngbBlastSearchService(dispatcher, projectContext, bamDataService, uiGridConstants);
    }

    _orderBy = null;
    _detailedRead = null;
    _columnsWidth = {'chr': 45, 'startIndex': 100, 'endIndex': 100, 'strand': 80};
    bamDataService;
    uiGridConstants;
    _blastHistory = [];
    _firstPageHistory = FIRST_PAGE;
    _lastPageHistory = FIRST_PAGE;
    _currentPageHistory = FIRST_PAGE;
    _totalPagesCountHistory = 0;
    _historyPageError = null;
    _currentResultId = null;
    _currentSearchId = null;

    get blastHistory() {
        return this._blastHistory;
    }

    get firstPageHistory() {
        return this._firstPageHistory;
    }

    set firstPageHistory(value) {
        this._firstPageHistory = value;
    }

    get lastPageHistory() {
        return this._lastPageHistory;
    }

    set lastPageHistory(value) {
        this._lastPageHistory = value;
    }

    get currentPageHistory() {
        return this._currentPageHistory;
    }

    set currentPageHistory(value) {
        this._currentPageHistory = value;
    }

    get historyPageSize() {
        return PAGE_SIZE_HISTORY;
    }

    get totalPagesCountHistory() {
        return this._totalPagesCountHistory;
    }

    set totalPagesCountHistory(value) {
        this._totalPagesCountHistory = value;
    }

    get historyPageError() {
        return this._historyPageError;
    }


    constructor(dispatcher, projectContext, bamDataService, uiGridConstants) {
        Object.assign(this, {dispatcher, projectContext, bamDataService, uiGridConstants});
        (async () => {
            this._blastHistory = await this.loadBlastHistory();
        })();
    }

    generateSpeciesList() {
        return [
            this.projectContext.reference,
            {id: '1eds52', name: 'GRCh38'},
            {id: '1adc47', name: 'Bacteria Escherichia coli'},
            {id: '4etr89', name: 'Clostridium botulinum'},
        ];
    }

    generateBlastSearchResults() {
        const results = [];
        this.projectContext.chromosomes.slice(0, 100).forEach(chr => {
            for (let i = 0; i < chr.size / 100; i++) {
                const start = 1 + Math.floor(Math.random() * (chr.size - 1));
                const singleSized = Math.random() >= 0.5;
                const end = Math.min(
                    start + (singleSized ? 1 : Math.floor(Math.random() * chr.size / 100)),
                    chr.size,
                );
                const numb = i+1;
                const chrName = `chr${chr.id}`;
                const strand = chr.id % 2 !== 0 ? 'POSITIVE' : 'NEGATIVE';
                const score = Math.floor(Math.random() * 100);
                const mismatch = Math.floor(Math.random() * 8);
                const match = 100 - mismatch;
                const taxid = Math.ceil(Math.random() * 4);
                results.push({
                    startIndex: start,
                    endIndex: end,
                    chromosome: chr.name,
                    chr: chrName,
                    strand,
                    score,
                    match,
                    mismatch,
                    numb,
                    taxid,
                });
            }
        });
        return results;
    }

    get blastRequest() {
        return JSON.parse(localStorage.getItem('blastSearchRequest')) || null;
    }

    get readSequence() {
        return this._detailedRead && this._detailedRead.sequence ? this._detailedRead.sequence : null;
    }

    async getDetailedRead(payload) {
        const read = await this.bamDataService.loadRead(payload);

        this._detailedRead = read ? read : null;

        return this._detailedRead;
    }

    async getBlastSearchResults() {
        const payload = this.blastRequest;

        if (!payload) return;

        await this.getDetailedRead(payload);

        const searchResults = await this.generateBlastSearchResults();

        return searchResults;
    }

    get blastColumns() {
        if (!localStorage.getItem('blastColumns')) {
            localStorage.setItem('blastColumns', JSON.stringify(DEFAULT_BLAST_COLUMNS));
        }
        let columns = JSON.parse(localStorage.getItem('blastColumns'));
        let defaultColumnsExists = true;
        for (let i = 0; i < DEFAULT_BLAST_COLUMNS.length; i++) {
            if (columns.map(c => c.toLowerCase()).indexOf(DEFAULT_BLAST_COLUMNS[i].toLowerCase()) === -1) {
                defaultColumnsExists = false;
                break;
            }
        }
        if (!defaultColumnsExists) {
            columns = DEFAULT_BLAST_COLUMNS.map(c => c);
            localStorage.setItem('blastColumns', JSON.stringify(columns || []));
        }
        return columns;
    }

    set blastColumns(columns) {
        localStorage.setItem('blastColumns', JSON.stringify(columns || []));
    }

    async loadBlastHistory() {
        let data = this._getRandomHistory(100);
        if (data.error) {
            this._totalPagesCountHistory = 0;
            this._currentPageHistory = FIRST_PAGE;
            this._lastPageHistory = FIRST_PAGE;
            this._firstPageHistory = FIRST_PAGE;
            this._hasMoreHistory = true;
            this._historyPageLoading = false;
            this._historyPageError = data.message;
            this.dispatcher.emit('blast:history:page:loading:finished');
            return [];
        } else {
            this._variantsPageError = null;
        }
        if (data.totalPagesCount === 0) {
            data.totalPagesCount = undefined;
        }
        data.forEach(item => item.isInProcess = item.currentState === blastSearchState.SEARCHING);
        return data;
    }

    get orderBy() {
        return this._orderBy;
    }

    set orderBy(orderBy) {
        this._orderBy = orderBy;
    }

    get columnsWidth() {
        return this._columnsWidth;
    }

    set columnsWidth(columnsWidth) {
        this._columnsWidth = columnsWidth;
    }

    get currentSearch() {
        const blastHistory = this.blastHistory;
        if (blastHistory) {
            return blastHistory.filter(item => item.id === this._currentSearchId)[0];
        }
        return undefined;
    }

    set currentSearchId(currentSearchId) {
        this._currentSearchId = currentSearchId;
    }

    set currentResultId(currentResultId) {
        this._currentResultId = currentResultId;
    }

    getBlastSearchGridColumns() {

        const headerCells = require('./ngbBlastSearchPanel_header.tpl.html');

        const result = [];
        const columnsList = this.blastColumns;

        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];

            let sortDirection = 0;
            if (this.orderBy) {
                const currentOrderByField = this.orderBy[0].field;
                const currentOrderByDirection = this.orderBy[0].desc ?
                    this.uiGridConstants.DESC : this.uiGridConstants.ASC;
                sortDirection = currentOrderByField === column ? currentOrderByDirection : 0;
            }

            result.push({
                enableHiding: false,
                field: column,
                headerCellTemplate: headerCells,
                minWidth: this.columnsWidth[column] ? this.columnsWidth[column] : 40,
                name: camelPad(column),
                width: '*',
                sort: {
                    direction: sortDirection
                },
            });
        }

        return result;
    }

    async deleteBlastHistory(id) {
        this._blastHistory = await this.loadBlastHistory();
    }

    async cancelBlastSearch(id) {
        this._blastHistory = await this.loadBlastHistory();
    }

    async clearSearchHistory() {
        this._blastHistory = await this.loadBlastHistory();
    }

    // TODO: remove before merge;
    _getRandomHistory(length) {
        const result = [];
        for (let i = 0; i < length; i++) {
            result.push({
                currentState: Object.keys(blastSearchState)[Math.floor(Math.random()*3)],
                duration: Math.round(Math.random()*1000 + 20),
                id: i+1,
                submitted: Date.now()+i*100000,
                title: Math.random() > 0.2 ? `History #${i+1}` : ''
            });
        }
        return result;
    }
}
