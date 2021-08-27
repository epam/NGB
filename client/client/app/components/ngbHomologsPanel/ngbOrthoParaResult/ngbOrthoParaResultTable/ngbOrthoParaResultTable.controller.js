import baseController from '../../../../shared/baseController';
import {Debounce} from '../../../../shared/utils/debounce';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbOrthoParaResultTableController extends baseController {
    dispatcher;
    isProgressShown = true;
    isEmptyResults = false;
    errorMessageList = [];
    searchResultTableLoadError = null;
    debounce = (new Debounce()).debounce;
    typeViewMap = {};
    gridOptions = {
        enableSorting: true,
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
        saveSelection: false,
        enablePaginationControls: false,
    };
    events = {
        'homologs:orthoPara:result:page:change': ::this.getDataOnPage
    };

    constructor($scope, $timeout, projectContext, projectDataService,
        ngbOrthoParaTableService, ngbOrthoParaResultService, ngbHomologsService, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            projectDataService,
            ngbOrthoParaTableService,
            ngbOrthoParaResultService,
            ngbHomologsService
        });

        this.initEvents();
        this.typeViewMap = this.ngbOrthoParaResultService.typeViewMap;
        this.$scope.$on('$destroy', () => {
            this.ngbHomologsService.isEmptyResults = true;
        });
    }

    static get UID() {
        return 'ngbOrthoParaResultTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        this.ngbOrthoParaResultService.currentPage = 1;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbOrthoParaResultService.getOrthoParaResultGridColumns(),
            paginationCurrentPage: this.ngbOrthoParaResultService.currentPage,
            paginationPageSize: this.ngbOrthoParaResultService.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.core.on.gridDimensionChanged(this.$scope, this.debounce(this, this.onResize.bind(this), RESIZE_DELAY));
                this.gridApi.core.on.renderingComplete(this.$scope, gridApi => {
                    this.debounce(this, this.onResize.bind(this), RESIZE_DELAY)(0, 0, gridApi.grid.gridHeight);
                });
            }
        });
        this.loadData();
    }

    loadData() {
        try {
            const result = this.ngbOrthoParaTableService.getOrthoParaResultById(this.ngbHomologsService.currentOrthoParaId);
            if (this.ngbOrthoParaTableService.searchResultTableError) {
                this.searchResultTableLoadError = this.ngbOrthoParaTableService.searchResultTableError;
                this.gridOptions.data = [];
                this.gridOptions.totalItems = 0;
                this.isEmptyResults = false;
            } else if (result.length) {
                this.searchResultTableLoadError = null;
                this.gridOptions.data = result;
                this.gridOptions.paginationPageSize = this.ngbOrthoParaResultService.pageSize;
                this.gridOptions.totalItems = result.length;
                this.isEmptyResults = false;
            } else {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
            this.ngbHomologsService.isEmptyResults = this.isEmptyResults;
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(() => this.$scope.$apply());
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    getDataOnPage(page) {
        this.ngbOrthoParaResultService.firstPage = page;
        if (this.gridApi) {
            this.gridApi.pagination.seek(page);
        }
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const orders = columns.map(c => c.name);
        const r = [];
        const names = this.ngbOrthoParaResultService.orthoParaResultColumns;
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
        this.ngbOrthoParaResultService.orthoParaResultColumns = result;
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
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
    }

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        const pageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 1;
        if (pageSize) {
            this.ngbOrthoParaResultService.pageSize = pageSize;
            this.ngbOrthoParaResultService.totalPages = Math.ceil(this.gridOptions.data.length / this.ngbOrthoParaResultService.pageSize);
            this.gridOptions.paginationPageSize = pageSize;
            this.$timeout(() => this.$scope.$apply());
        }
    }

    async navigateToTrack(entity) {
        let coordinates = null;
        if (entity.accession_id) {
            coordinates = await this.projectDataService.getFeatureCoordinates(entity.accession_id, 'PROTEIN', entity.taxId);
        }
        if (coordinates && !coordinates.error) {
            const range = Math.abs(coordinates.end - coordinates.start);
            const start = Math.min(coordinates.start, coordinates.end) - range / 10.0;
            const end = Math.max(coordinates.start, coordinates.end) + range / 10.0;
            this.projectContext.changeState({
                chromosome: {id: coordinates.chromosomeId},
                viewport: {
                    start,
                    end
                }
            });
            // navigate to track
        } else {
            event.stopImmediatePropagation();
            window.open(`https://www.ncbi.nlm.nih.gov/gene/${entity.geneId}`);
            return false;
        }
    }

}
