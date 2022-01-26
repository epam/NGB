import baseController from '../../../shared/baseController';
import {Debounce} from '../../../shared/utils/debounce';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbInternalPathwaysTableController extends baseController {
    dispatcher;
    isProgressShown = true;
    isEmptyResult = false;
    errorMessageList = [];
    debounce = (new Debounce()).debounce;
    gridOptions = {
        enableSorting: false,
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        height: '100%',
        multiSelect: false,
        rowHeight: ROW_HEIGHT,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        saveWidths: true,
        saveOrder: true,
        saveScroll: false,
        saveFocus: false,
        saveVisible: true,
        saveSort: false,
        saveFilter: false,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false
    };
    events = {
        'pathways:internalPathways:page:change': this.loadData.bind(this),
        'pathways:internalPathways:search': this.loadData.bind(this),
        'read:show:pathways': this.loadData.bind(this)
    };

    constructor($scope, $timeout, dispatcher,
        ngbInternalPathwaysTableService, ngbPathwaysService, uiGridConstants) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbInternalPathwaysTableService,
            ngbPathwaysService,
            uiGridConstants,
        });

        this.initEvents();
    }

    static get UID() {
        return 'ngbInternalPathwaysTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbInternalPathwaysTableService.getInternalPathwaysGridColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.core.on.gridDimensionChanged(this.$scope, this.debounce(this, this.onResize.bind(this), RESIZE_DELAY));
                this.gridApi.core.on.renderingComplete(this.$scope, gridApi => {
                    this.debounce(this, this.onResize.bind(this), RESIZE_DELAY)(0, 0, gridApi.grid.gridHeight);
                });
            }
        });
        await this.loadData();
    }

    async loadData() {
        this.isProgressShown = true;
        try {
            await this.ngbInternalPathwaysTableService.searchInternalPathways(this.ngbPathwaysService.currentSearch);
            const dataLength = this.ngbInternalPathwaysTableService.internalPathways.length;
            if (this.ngbInternalPathwaysTableService.pageError) {
                this.errorMessageList = [this.ngbInternalPathwaysTableService.pageError];
                this.gridOptions.data = [];
                this.isEmptyResults = false;
            } else if (dataLength) {
                this.errorMessageList = [];
                this.gridOptions.data = this.ngbInternalPathwaysTableService.internalPathways;
                this.gridOptions.totalItems = dataLength;
                this.isEmptyResults = false;
            } else {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
        } catch (errorObj) {
            this.isProgressShown = false;
            this.onError(errorObj.message);
        }
        this.$timeout(() => this.$scope.$apply());
    }

    onError(message) {
        this.errorMessageList = [message];
    }

    rowClick(row, event) {
        const entity = row.entity;
        if (entity) {
            this.ngbPathwaysService.currentInternalPathwaysId = row.entity.xml;
            this.changeState({state: 'INTERNAL_PATHWAYS_RESULT'});
        } else {
            event.stopImmediatePropagation();
            return false;
        }
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const fieldTitleMap = (
            o => Object.keys(o).reduce(
                (r, k) => Object.assign(r, { [o[k]]: k }), {}
            )
        )(this.ngbInternalPathwaysTableService.columnTitleMap);
        const mapNameToField = function ({name}) {
            return fieldTitleMap[name];
        };
        const orders = columns.map(mapNameToField);
        const r = [];
        const names = this.ngbInternalPathwaysTableService.internalPathwaysColumns;
        for (const name of names) {
            r.push(orders.indexOf(name) >= 0);
        }
        let index = 0;
        const result = [];
        for (let i = 0; i < r.length; i++) {
            if (r[i]) {
                result.push(orders[index]);
                index++;
            } else {
                result.push(names[i]);
            }
        }
        this.ngbInternalPathwaysTableService.internalPathwaysColumns = result;
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.ngbInternalPathwaysTableService.orderBy = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: this.ngbInternalPathwaysTableService.orderByColumns[sc.field] || sc.field
            }));
        } else {
            this.ngbInternalPathwaysTableService.orderBy = null;
        }

        this.ngbInternalPathwaysTableService.currentPage = 1;
        this.gridOptions.data = [];
        const sortingConfiguration = sortColumns
            .filter(column => !!column.sort)
            .map((column, priority) => ({
                field: column.field,
                sort: ({
                    ...column.sort,
                    priority
                })
            }));
        const {columns = []} = grid || {};
        columns.forEach(columnDef => {
            const [sortingConfig] = sortingConfiguration
                .filter(c => c.field === columnDef.field);
            if (sortingConfig) {
                columnDef.sort = sortingConfig.sort;
            }
        });
        this.loadData();
    }

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        const pageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 2;
        if (pageSize) {
            this.ngbInternalPathwaysTableService.pageSize = pageSize;
            this.loadData();
        }
    }
}
