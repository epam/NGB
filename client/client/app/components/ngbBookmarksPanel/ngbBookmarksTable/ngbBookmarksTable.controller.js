import  baseController from '../../../shared/baseController';

export default class ngbBookmarksTableController extends baseController {
    static get UID() {
        return 'ngbBookmarksTableController';
    }

    isDataLoaded = false;
    isNothingFound = false;

    gridOptions = {
        enableHorizontalScrollbar:0,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 48,
        height: '100%',
        multiSelect: false,
        rowHeight: 35,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false
    };

    dispatcher;
    projectContext;
    trackNamingService;

    events = {
        'bookmark:save': this.loadData.bind(this)
    };

    constructor($scope, bookmarksTableService, dispatcher, projectContext, $mdDialog, trackNamingService) {
        super();

        Object.assign(this, {$scope, bookmarksTableService, dispatcher, projectContext, $mdDialog, trackNamingService});

        $scope.$watch('$ctrl.searchPattern', this.loadData.bind(this));

        this.initEvents();
    }

    $onInit() {
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
            },
            ...this.bookmarksTableService.getBookmarksGridColumns()
        });
        this.loadData();
    }

    loadData() {
        const bookmarks = this.bookmarksTableService.loadBookmarks() || [];
        if (this.searchPattern && this.searchPattern.length) {
            this.gridOptions.data = bookmarks.filter(bookmark => bookmark.name.toLowerCase().indexOf(this.searchPattern.toLowerCase()) >= 0);
        } else {
            this.gridOptions.data = bookmarks;
        }
        this.isNothingFound = !this.gridOptions.data || !this.gridOptions.data.length;
        this.isDataLoaded=true;
    }

    rowClick(row) {
        const entity = row.entity;
        const position = {
            end: entity.endIndex,
            start: entity.startIndex
        };
        const chromosomeName = `${entity.chromosome.name}`.toLowerCase();
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
        if (customNames) {
            this.trackNamingService.setCustomNames(customNames);
        }
        this.projectContext.changeState({chromosome: {name: chromosomeName}, viewport: position, tracksState, layout, forceVariantsFilter: true});
    }

    onRemove(row, event) {
        const entity = row.entity;
        const id = entity.id;

        const confirm = this.$mdDialog.confirm()
            .title(`Delete session ${entity.name}?`)
            .ok('OK')
            .cancel('CANCEL');

        this.$mdDialog.show(confirm).then(async() => {
            this.bookmarksTableService.deleteBookmark(id);
            this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }


}