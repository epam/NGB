import baseController from '../../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbBlastSearchResultTableController extends baseController {
    static get UID() {
        return 'ngbBlastSearchResultTableController';
    }

    dispatcher;
    projectContext;

    isProgressShown = true;
    errorMessageList = [];
    searchResultTableLoadError = null;

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
    };

    constructor($scope, $timeout, ngbBlastSearchResultTableService, ngbBlastSearchService, dispatcher, projectContext) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchResultTableService,
            ngbBlastSearchService,
            projectContext
        });

        this.initEvents();
    }

    $onInit() {
        this.initialize();
    }

    // refreshScope(needRefresh) {
    //     if (needRefresh) {
    //         this.$scope.$apply();
    //     }
    // }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbBlastSearchResultTableService.getBlastSearchResultGridColumns(),
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                // this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                // this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                // this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
            }
        });
        await this.loadData();
    }

    async loadData() {
        try {
            await this.ngbBlastSearchResultTableService.updateSearchResult(this.ngbBlastSearchService.currentResultId);
            if (this.ngbBlastSearchResultTableService.blastSearchResult.length || this.ngbBlastSearchResultTableService.searchResultTableError) {
                this.searchResultLoadingFinished();
            }
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(::this.$scope.$apply);
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    /*
        saveColumnsState() {
            if (!this.gridApi) {
                return;
            }
            const {columns} = this.gridApi.saveState.save();
            const mapNameToField = function ({name}) {
                switch (name) {
                    case 'Type':
                        return 'variationType';
                    case 'Chr':
                        return 'chrName';
                    case 'Gene':
                        return 'geneNames';
                    case 'Position':
                        return 'startIndex';
                    case 'Info':
                        return 'info';
                    default:
                        return name;
                }
            };
            const orders = columns.map(mapNameToField);
            const r = [];
            const names = this.projectContext.vcfColumns;
            for (let i = 0; i < names.length; i++) {
                const name = names[i];
                if (orders.indexOf(name) >= 0) {
                    r.push(1);
                } else {
                    r.push(0);
                }
            }
            let index = 0;
            const result = [];
            for (let i = 0; i < r.length; i++) {
                if (r[i] === 1) {
                    result.push(orders[index]);
                    index++;
                } else {
                    result.push(names[i]);
                }
            }
            this.projectContext.vcfColumns = result;
        }
    */
    searchResultLoadingFinished() {
        this.searchResultTableLoadError = null;
        this.gridOptions.columnDefs = this.ngbBlastSearchResultTableService.getBlastSearchResultGridColumns();
        this.gridOptions.data = this.ngbBlastSearchResultTableService.blastSearchResult;
        this.isProgressShown = false;

        this.$timeout(::this.$scope.$apply);
    }
}
