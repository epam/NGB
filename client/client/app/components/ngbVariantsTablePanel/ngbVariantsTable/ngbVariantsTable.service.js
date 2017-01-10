export default class ngbVariantsTableService {

    static instance(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants) {
        return new ngbVariantsTableService(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants);
    }

    columnTypes = {
        flag: 'Flag',
        integer: 'Integer',
        string: 'String'
    };

    constructor(genomeDataService, projectDataService, variantsTableMessages, uiGridConstants) {
        Object.assign(this, {genomeDataService, projectDataService, variantsTableMessages, uiGridConstants});
    }

    getVariantsGridColumns(columnsList = []) {

        const infoCells = require('./ngbVariantsTable_actions.tpl.html');
        const headerCells = require('./ngbVariantsTable_header.tpl.html');


        return [
            {
                cellTemplate: `<div class="md-label variation-type" 
                                    md-colors="{background: 'accent-{{COL_FIELD CUSTOM_FILTERS}}',color:'background-900'}" 
                                    ng-class="COL_FIELD CUSTOM_FILTERS" >{{COL_FIELD CUSTOM_FILTERS}}</div>`,
                enableHiding: false,
                field: 'variationType',
                filter: {
                    selectOptions: [
                        {label: 'DEL', value: 'DEL'},
                        {label: 'INS', value: 'INS'},
                        {label: 'SNV', value: 'SNV'},
                        {label: 'BND', value: 'BND'},
                        {label: 'INV', value: 'INV'},
                        {label: 'DUP', value: 'DUP'},
                        {label: 'UNK', value: 'UNK'}],
                    term: '',
                    type: this.uiGridConstants.filter.SELECT
                },
                headerCellTemplate: headerCells,
                maxWidth: 104,
                minWidth: 104,
                name: 'Type'
            },
            {
                enableHiding: false,
                field: 'chrName',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Chr',
                width: '*'
            },
            {
                enableHiding: false,
                field: 'geneNames',
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Gene',
                width: '*',
            },
            {
                enableHiding: false,
                field: 'startIndex',
                filters: [
                    {
                        condition: this.uiGridConstants.filter.GREATER_THAN,
                        placeholder: 'greater than'
                    },
                    {
                        condition: this.uiGridConstants.filter.LESS_THAN,
                        placeholder: 'less than'
                    }
                ],
                headerCellTemplate: headerCells,
                minWidth: 50,
                name: 'Position',
                width: '*'
            },
            ...columnsList.map(column =>
                ({
                    field: column.name,
                    filters: (() => {
                        if (column.type === this.columnTypes.integer) {
                            return [
                                {
                                    condition: this.uiGridConstants.filter.GREATER_THAN,
                                    placeholder: 'greater than'
                                },
                                {
                                    condition: this.uiGridConstants.filter.LESS_THAN,
                                    placeholder: 'less than'
                                }];
                        }

                    })(),
                    headerCellTemplate: headerCells,
                    minWidth: 50,
                    name: column.name,
                    width: '*'
                })
            ),
            {
                cellTemplate: infoCells,
                enableColumnMenu: false,
                enableSorting: false,
                field: 'info',
                headerCellTemplate: headerCells,
                maxWidth: 60,
                minWidth: 60,
                name: 'Info'
            }
        ];
    }
}