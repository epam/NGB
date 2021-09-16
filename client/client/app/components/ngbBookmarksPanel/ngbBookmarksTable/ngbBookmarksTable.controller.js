import baseController from '../../../shared/baseController';

export default class ngbBookmarksTableController extends baseController {
    static get UID() {
        return 'ngbBookmarksTableController';
    }

    isDataLoaded = false;
    isNothingFound = false;
    isInitialized = false;

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

    constructor($scope, ngbBookmarksTableService, dispatcher, projectContext, miewContext, $mdDialog, trackNamingService) {
        super();
        Object.assign(
            this,
            {
                $scope,
                ngbBookmarksTableService,
                dispatcher,
                projectContext,
                $mdDialog,
                trackNamingService,
                miewContext
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
        this.ngbBookmarksTableService.firstPage = page;
        if (this.gridApi) {
            this.gridApi.pagination.seek(page);
        }
    }

    rowClick(row) {
        const sessionsDefaultTab = {
            'o': 'Sessions',
            'l': '5',
            'h': 'angularModule',
            'i': {
                'displayed': '1',
                'icon': 'bookmark_outline',
                'panel': 'ngbBookmarksPanel',
                'position': 'left',
                'o': 'Sessions',
                'name': 'layout>bookmark',
                'key': 'bookmark',
                'iconColor': 'accent-100',
                'hotkey': 'ALT + B',
                'htmlModule': 'ngb-bookmarks-panel'
            },
            'n': '0',
            't': '0'
        };
        const sessionsDefaultPanel = {
            'l': '4',
            'k': 33.33333333333333,
            'i': {
                'position': 'left'
            },
            'n': '0',
            't': '0',
            'o': '',
            's': 0,
            'g': [sessionsDefaultTab]
        };
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
        if (layout && layout.g && layout.g[0] && layout.g[0].g) {
            const isSession = layout.g[0].g.some(gg => gg.g ? gg.g.some(ggg => ggg.o === 'Sessions') : false);
            const NEW_LAYOUT_WIDTH_PERCENTAGE = 0.6666;
            if (!isSession) {
                const leftPanelIndex = layout.g[0].g.findIndex(g => g.i && g.i.position === 'left');
                if (~leftPanelIndex) {
                    layout.g[0].g[leftPanelIndex].g.unshift(sessionsDefaultTab);
                } else {
                    layout.g[0].g.forEach(g => {
                        g.k *= NEW_LAYOUT_WIDTH_PERCENTAGE;
                    });
                    layout.g[0].g.unshift(sessionsDefaultPanel);
                }
            }
        }
        const vcfColumns = entity.vcfColumns;
        if (vcfColumns) {
            this.projectContext.vcfColumns = vcfColumns;
        }
        const customNames = entity.customNames;
        if (customNames) {
            this.trackNamingService.setCustomNames(customNames);
        }
        this.miewContext.routeInfo = entity.miew;
        this.projectContext.changeState({
            chromosome: chromosomeName ? {name: chromosomeName} : undefined,
            viewport: position,
            tracksState,
            layout,
            forceVariantsFilter: true
        });
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
}
