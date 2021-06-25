export default class ngbVariantsTableService {

    static instance(projectContext, genomeDataService, projectDataService, variantsTableMessages, uiGridConstants) {
        return new ngbVariantsTableService(projectContext, genomeDataService, projectDataService, variantsTableMessages, uiGridConstants);
    }

    columnTypes = {
        flag: 'Flag',
        integer: 'Integer',
        string: 'String'
    };

    projectContext;

    constructor(projectContext, genomeDataService, projectDataService, variantsTableMessages, uiGridConstants) {
        Object.assign(this, {projectContext, genomeDataService, projectDataService, variantsTableMessages, uiGridConstants});
    }

    getVariantsGridColumns() {

        const infoCells = require('./ngbVariantsTable_actions.tpl.html');
        const headerCells = require('./ngbVariantsTable_header.tpl.html');

        const columnsList = this.projectContext.vcfColumns;
        const visibleColumns = this.projectContext.vcfInfo.map(c => c.name);
        const result = [];

        const self = this;
        let columnSettings = null;
        let sortDirection = 0;

        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            columnSettings = null;
            const nameOrderByColumn = this.projectContext.orderByColumnsVariations[column] ? this.projectContext.orderByColumnsVariations[column] : column;
            if (this.projectContext.orderByVariations) {
                const currentOrderByFieldVariations = this.projectContext.orderByVariations[0].field;
                const currentOrderByDirectionVariations = this.projectContext.orderByVariations[0].desc ? 'desc' : 'asc';
                sortDirection = currentOrderByFieldVariations === nameOrderByColumn ? currentOrderByDirectionVariations : 0;
            }
            switch (column) {
                case 'variationType': {
                    columnSettings = {
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
                                {label: 'MNP', value: 'MNP'},
                                {label: 'UNK', value: 'UNK'}],
                            term: '',
                            type: this.uiGridConstants.filter.SELECT
                        },
                        headerCellTemplate: headerCells,
                        maxWidth: 104,
                        minWidth: 104,
                        name: 'Type',
                        filterApplied: () => {
                            return this.projectContext.variantsFieldIsFiltered(column);
                        },
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.projectContext.clearVariantFieldFilter(column),
                                shown: () => this.projectContext.variantsFieldIsFiltered(column)
                            }
                        ]
                    };
                }
                    break;
                case 'chrName': {
                    columnSettings = {
                        enableHiding: false,
                        field: 'chrName',
                        headerCellTemplate: headerCells,
                        minWidth: 50,
                        name: 'Chr',
                        width: '*',
                        filterApplied: () => {
                            return this.projectContext.variantsFieldIsFiltered(column);
                        },
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.projectContext.clearVariantFieldFilter(column),
                                shown: () => this.projectContext.variantsFieldIsFiltered(column)
                            }
                        ]
                    };
                }
                    break;
                case 'geneNames': {
                    columnSettings = {
                        enableHiding: false,
                        field: 'geneNames',
                        headerCellTemplate: headerCells,
                        minWidth: 50,
                        name: 'Gene',
                        width: '*',
                        filterApplied: () => {
                            return this.projectContext.variantsFieldIsFiltered(column);
                        },
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.projectContext.clearVariantFieldFilter(column),
                                shown: () => this.projectContext.variantsFieldIsFiltered(column)
                            }
                        ]
                    };
                }
                    break;
                case 'startIndex': {
                    columnSettings = {
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
                        width: '*',
                        filterApplied: () => {
                            return this.projectContext.variantsFieldIsFiltered(column);
                        },
                        menuItems: [
                            {
                                title: 'Clear column filter',
                                action: () => this.projectContext.clearVariantFieldFilter(column),
                                shown: () => this.projectContext.variantsFieldIsFiltered(column)
                            }
                        ]
                    };
                }
                    break;
                case 'info': {
                    columnSettings = {
                        cellTemplate: infoCells,
                        enableColumnMenu: false,
                        enableSorting: false,
                        enableMove: false,
                        field: 'info',
                        headerCellTemplate: headerCells,
                        maxWidth: 60,
                        minWidth: 60,
                        name: 'Info'
                    };
                }
                    break;
                default: {
                    const [infoColumn] = this.projectContext.vcfInfo.filter(c => c.name === column);
                    if (infoColumn) {
                        columnSettings = {
                            field: infoColumn.name,
                            filters: (() => {
                                if (infoColumn.type === this.columnTypes.integer) {
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
                            name: infoColumn.name,
                            width: '*',
                            visible: (() => {
                                return visibleColumns.indexOf(infoColumn.name) !== -1;
                            })(),
                            filterApplied: () => {
                                return this.projectContext.variantsFieldIsFiltered(column);
                            },
                            menuItems: [
                                {
                                    title: 'Clear column filter',
                                    action: () => this.projectContext.clearVariantFieldFilter(column),
                                    shown: () => this.projectContext.variantsFieldIsFiltered(column)
                                }
                            ]
                        };
                    }
                }
                    break;
            }
            if (columnSettings) {
                if (sortDirection) {
                    columnSettings.sort = {
                        direction: sortDirection
                    };
                }
                result.push(columnSettings);
            }
        }

        return result;
    }
}
