import {camelPad} from '../../shared/utils/String.js';

const DEFAULT_BLAST_COLUMNS = [
    'numb', 'chr', 'startIndex', 'endIndex', 'strand', 'score',
    'match', 'mismatch', 'repMatch',
    'ns',
    'qGapCount', 'qGapBases',
    'tGapCount', 'tGapBases',
];

export default class ngbBlastSearchService {
    static instance(projectContext, bamDataService, uiGridConstants){
        return new ngbBlastSearchService(projectContext, bamDataService, uiGridConstants);
    }

    _orderBy = null;
    _detailedRead = null;
    _columnsWidth = { 'numb': 50, 'chr' : 45, 'startIndex' : 100, 'endIndex' : 100, 'strand' : 80 };
    bamDataService;
    uiGridConstants;

    constructor(projectContext, bamDataService, uiGridConstants){
        Object.assign(this, { projectContext, bamDataService, uiGridConstants });
    }
    generateSpeciesList() {
        return [
            {...this.projectContext.reference, taxid: 1},
            {id:'1eds52', name:'GRCh38', taxid: 2},
            {id:'1adc47', name:'Bacteria Escherichia coli', taxid: 3},
            {id:'4etr89', name:'Clostridium botulinum', taxid: 4},
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

    async getBlastSearchResults(){
        const payload = this.blastRequest;

        if(!payload) return;

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

    getBlastSearchGridColumns() {

        const headerCells = require('./ngbBlastSearchPanel_header.tpl.html');

        const result = [];
        const columnsList = this.blastColumns;

        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];

            let sortDirection = 0;
            if(this.orderBy) {
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
}
