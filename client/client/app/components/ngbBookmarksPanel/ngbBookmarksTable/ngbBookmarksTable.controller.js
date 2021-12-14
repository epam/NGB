import baseController from '../../../shared/baseController';
import {Debounce} from '../../../shared/utils/debounce';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbBookmarksTableController extends baseController {
    static get UID() {
        return 'ngbBookmarksTableController';
    }

    isDataLoaded = false;
    isNothingFound = false;
    isInitialized = false;
    debounce = (new Debounce()).debounce;

    gridOptions = {
        enableHorizontalScrollbar: 0,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 48,
        height: '100%',
        multiSelect: false,
        rowHeight: 35,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        enablePaginationControls: false,
    };

    dispatcher;
    projectContext;
    trackNamingService;

    events = {
        'bookmarks:save': this.loadData.bind(this),
        'bookmarks:refresh': () => {
            this.isDataLoaded = false;
            return this.loadData();
        },
        'display:bookmarks:filter': this.refreshScope.bind(this),
        'bookmarks:page:change': this.getDataOnPage.bind(this),
        'reference:change': () => {
            if (this.gridOptions.data && this.projectContext.reference && this.isInitialized) {
                this.isDataLoaded = true;
                this.$scope.$apply();
            }
        },
        'ngb:init:finished': () => {
            this.isInitialized = true;
            if (this.gridOptions.data) {
                this.isDataLoaded = true;
                this.$scope.$apply();
            }
        }
    };

    constructor(
        $scope,
        $timeout,
        ngbBookmarksTableService,
        dispatcher,
        projectContext,
        miewContext,
        heatmapContext,
        $mdDialog,
        trackNamingService,
        appLayout,
        ngbStrainLineageService
    ) {
        super();
        Object.assign(
            this,
            {
                $scope,
                $timeout,
                ngbBookmarksTableService,
                dispatcher,
                projectContext,
                $mdDialog,
                heatmapContext,
                trackNamingService,
                miewContext,
                appLayout,
                ngbStrainLineageService
            });
        this.displayBookmarksFilter = this.ngbBookmarksTableService.displayBookmarksFilter;
        if (this.projectContext.references.length) {
            this.isInitialized = true;
        }
        this.initEvents();
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbBookmarksTableService.getBookmarksGridColumns(),
            paginationCurrentPage: this.ngbBookmarksTableService.currentPage,
            paginationPageSize: this.ngbBookmarksTableService.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
                this.gridApi.core.on.gridDimensionChanged(this.$scope, this.debounce(this, this.onResize.bind(this), RESIZE_DELAY));
                this.gridApi.core.on.renderingComplete(this.$scope, gridApi => {
                    this.debounce(this, this.onResize.bind(this), RESIZE_DELAY)(0, 0, gridApi.grid.gridHeight);
                });
            }
        });
        this.loadData();
    }

    async loadData() {
        const bookmarks = await this.ngbBookmarksTableService.loadBookmarks();
        if (this.ngbBookmarksTableService.pageError) {
            this.resultTableError = this.ngbBookmarksTableService.pageError;
            this.gridOptions.data = [];
            this.gridOptions.totalItems = 0;
            this.isNothingFound = false;
        } else if (bookmarks.length) {
            this.resultTableError = null;
            this.gridOptions.data = bookmarks;
            this.gridOptions.paginationPageSize = this.ngbBookmarksTableService.pageSize;
            this.gridOptions.totalItems = bookmarks.length;
            this.isNothingFound = false;
        } else {
            this.resultTableError = null;
            this.gridOptions.data = [];
            this.gridOptions.totalItems = 0;
            this.isNothingFound = true;
        }
        if (this.isInitialized) {
            this.isDataLoaded = true;
            this.$scope.$apply();
        }
    }

    getDataOnPage(page) {
        if (this.gridApi) {
            this.gridApi.pagination.seek(page);
        }
    }

    rowClick(row) {
        const entity = row.entity;
        const position = entity.startIndex && entity.endIndex
            ? {
                end: entity.endIndex,
                start: entity.startIndex
            }
            : undefined;
        const chromosomeName = entity.chromosome
            ? `${entity.chromosome.name}`.toLowerCase()
            : undefined;
        const tracksState = entity.tracks;
        tracksState.forEach(t => {
            if (t.projectId === '') {
                t.isLocal = true;
            }
        });
        const layout = entity.layout;
        const vcfColumns = entity.vcfColumns;
        if (vcfColumns) {
            this.projectContext.vcfColumns = vcfColumns;
        }
        const customNames = entity.customNames;
        this.dispatcher.emitGlobalEvent('session:load:started', {layoutChange: this.appLayout.Panels.bookmark});
        if (customNames) {
            this.trackNamingService.setCustomNames(customNames);
        }
        this.miewContext.routeInfo = entity.miew;
        this.heatmapContext.routeInfo = entity.heatmap;
        this.ngbStrainLineageService.recoverLocalState(entity.lineage);
        this.projectContext.changeState({
            chromosome: chromosomeName ? {name: chromosomeName} : undefined,
            viewport: position,
            tracksState,
            layout,
            forceVariantsFilter: true
        }, false, this.openBookmarksPanel.bind(this));
    }

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        const pageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 1;
        if (pageSize) {
            this.ngbBookmarksTableService.pageSize = pageSize;
            this.ngbBookmarksTableService.totalPages = Math.ceil(this.gridOptions.data.length / this.ngbBookmarksTableService.pageSize);
            this.gridOptions.paginationPageSize = pageSize;
            this.$timeout(() => this.$scope.$apply());
        }
    }

    onRemove(row, event) {
        const entity = row.entity;
        const id = entity.id;
        const isLocal = entity.isLocal;

        const confirm = this.$mdDialog.confirm()
            .title(`Delete session ${entity.name}?`)
            .ok('OK')
            .cancel('CANCEL');

        this.$mdDialog.show(confirm).then(async () => {
            this.ngbBookmarksTableService.deleteBookmark(id, isLocal).then(this.loadData.bind(this));
        });
        event.stopImmediatePropagation();
        return false;
    }

    refreshScope(needRefresh) {
        if (needRefresh) {
            this.$scope.$apply();
        }
    }

    openBookmarksPanel() {
        this.appLayout.Panels.bookmark.displayed = true;
        this.dispatcher.emitGlobalEvent('layout:item:change', {layoutChange: this.appLayout.Panels.bookmark});
    }
}
