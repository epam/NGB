import {camelPad} from '../../../shared/utils/String';

const DEFAULT_BLAST_HISTORY_COLUMNS = [
    'id', 'title', 'currentState', 'submitted', 'duration', 'actions'
];

export default class ngbBlastHistoryTableService {

    static instance() {
        return new ngbBlastHistoryTableService();
    }

    constructor() {
    }

    get blastHistoryColumns() {
        return DEFAULT_BLAST_HISTORY_COLUMNS;
    }


    getBlastHistoryGridColumns() {
        const actionsCell = require('./ngbBlastHistoryTable_actions.tpl.html');
        const headerCells = require('./ngbBlastHistoryTable_header.tpl.html');

        const result = [];
        const columnsList = this.blastHistoryColumns;
        for (let i = 0; i < columnsList.length; i++) {
            const column = columnsList[i];
            switch (column) {
                case 'id': {
                    result.push({
                        cellTemplate: `<div ng-if="!row.entity.isInProcess"
                                        class="ui-grid-cell-contents search-result-link" 
                                        ng-click="grid.appScope.$ctrl.showResult(row.entity, $event)"
                                       >{{row.entity.id}}</div>
                                       <div ng-if="row.entity.isInProcess" 
                                        class="ui-grid-cell-contents search-result-in-progress"
                                       >{{row.entity.id}}</div>`,
                        enableHiding: false,
                        field: 'id',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'id'
                    });
                    break;
                }
                case 'submitted': {
                    result.push({
                        cellFilter: 'date:"short"',
                        enableHiding: false,
                        field: 'submitted',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'submitted'
                    });
                    break;
                }
                case 'duration': {
                    result.push({
                        cellFilter: 'duration:this',
                        enableHiding: false,
                        field: 'duration',
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: 'duration'
                    });
                    break;
                }
                case 'actions': {
                    result.push({
                        cellTemplate: actionsCell,
                        enableSorting: false,
                        field: 'id',
                        headerCellTemplate: '<span></span>',
                        maxWidth: 96,
                        minWidth: 64,
                        name: ''
                    });
                    break;
                }
                default: {
                    result.push({
                        enableHiding: false,
                        field: column,
                        headerCellTemplate: headerCells,
                        minWidth: 40,
                        name: camelPad(column),
                        width: '*'
                    });
                    break;
                }
            }
        }
        return result;
    }
}
