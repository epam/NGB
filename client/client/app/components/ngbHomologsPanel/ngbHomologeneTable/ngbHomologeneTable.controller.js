import {Debounce} from '../../../shared/utils/debounce';
import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbHomologeneTableController extends baseController {
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
        'homologs:homologene:page:change': this.getDataOnPage.bind(this),
        'read:show:homologs': this.loadData.bind(this)
    };

    constructor($scope, $timeout, dispatcher,
        ngbHomologeneTableService, ngbHomologsService, uiGridConstants) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbHomologeneTableService,
            ngbHomologsService,
            uiGridConstants,
        });

        this.initEvents();
    }

    static get UID() {
        return 'ngbHomologeneTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbHomologeneTableService.getHomologeneGridColumns(),
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
            await this.ngbHomologeneTableService.searchHomologene(this.ngbHomologsService.currentSearch.search);
            const dataLength = this.ngbHomologeneTableService.homologene.length;
            if (this.ngbHomologeneTableService.pageError) {
                this.errorMessageList = [this.ngbHomologeneTableService.pageError];
                this.gridOptions.data = [];
                this.isEmptyResults = false;
            } else if (dataLength) {
                this.errorMessageList = [];
                this.gridOptions.data = this.ngbHomologeneTableService.homologene;
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
            this.ngbHomologsService.currentHomologeneId = row.entity.groupId;
            this.changeState({state: 'HOMOLOGENE_RESULT'});
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
        )(this.ngbHomologeneTableService.columnTitleMap);
        const mapNameToField = function ({name}) {
            return fieldTitleMap[name];
        };
        const orders = columns.map(mapNameToField);
        const r = [];
        const names = this.ngbHomologeneTableService.homologeneColumns;
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
        this.ngbHomologeneTableService.homologeneColumns = result;
    }

    getDataOnPage(page) {
        this.ngbHomologeneTableService.firstPage = page;
        return this.loadData();
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.ngbHomologeneTableService.orderBy = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: this.ngbHomologeneTableService.orderByColumns[sc.field] || sc.field
            }));
        } else {
            this.ngbHomologeneTableService.orderBy = null;
        }

        this.ngbHomologeneTableService.currentPage = 1;
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
            this.ngbHomologeneTableService.pageSize = pageSize;
            this.loadData();
        }
    }
}
