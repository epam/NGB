const COVERAGE_TABLE_COLUMNS = ['chr', 'start', 'end', 'coverage'];

export default class ngbCoverageTableService {

    get coverageTableColumns() {
        return COVERAGE_TABLE_COLUMNS;
    }

    static instance(ngbCoveragePanelService) {
        return new ngbCoverageTableService(ngbCoveragePanelService);
    }

    columnTypes = {
        flag: 'Flag',
        integer: 'Integer',
        string: 'String'
    };

    constructor(ngbCoveragePanelService) {
        Object.assign(this, {ngbCoveragePanelService});
    }

    get sortInfo () {
        return this.ngbCoveragePanelService.sortInfo;
    }
    set sortInfo (value) {
        this.ngbCoveragePanelService.sortInfo = value;
    }

    getMotifsResultsGridColumns() {
        const headerCells = require('./ngbCoverageTable_header.tpl.html');

        const result = [];
        const columnsList = this.coverageTableColumns;

        for (let i = 0; i < columnsList.length; i++) {
            let columnSettings = null;
            let sortDirection = 0;
            let sortingPriority = 0;
            const column = columnsList[i];
            if (this.sortInfo) {
                const [columnSortingConfiguration] = this.sortInfo.filter(o => o.field === column);
                if (columnSortingConfiguration) {
                    sortingPriority = this.sortInfo.indexOf(columnSortingConfiguration);
                    sortDirection = columnSortingConfiguration.ascending ? 'asc' : 'desc';
                }
            }
            columnSettings = {
                enableHiding: false,
                enableFiltering: false,
                enableSorting: true,
                field: column,
                headerCellTemplate: headerCells,
                headerTooltip: column,
                minWidth: 40,
                displayName: column,
                width: '*'
            };
            if (columnSettings) {
                if (sortDirection) {
                    columnSettings.sort = {
                        direction: 'asc',
                        priority: sortingPriority
                    };
                }
                result.push(columnSettings);
            }
        }
        return result;
    }
}
