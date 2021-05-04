import {EventVariationInfo} from '../../../shared/utils/events';
import VcfHighlightConditionService from '../../../../dataServices/utils/vcf-highlight-condition-service';
import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbVariantsTableController extends baseController {

    static get UID() {
        return 'ngbVariantsTableController';
    }

    dispatcher;
    projectContext;
    variantsTableMessages;

    isProgressShown = true;
    errorMessageList = [];
    variantsLoadError = null;
    highlightProfile = {};

    gridOptions = {
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 20,
        height: '100%',
        infiniteScrollDown: true,
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: true,
        multiSelect: false,
        rowHeight: ROW_HEIGHT,
        saveFilter: false,
        saveFocus: false,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveOrder: true,
        savePinning: true,
        saveScroll: false,
        saveSelection: false,
        saveSort: true,
        saveTreeView: false,
        saveVisible: true,
        saveWidths: true,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false
    };

    /* @ngInject */
    constructor($scope, $timeout, variantsTableMessages, variantsTableService, uiGridConstants, dispatcher, projectContext, localDataService) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            uiGridConstants,
            variantsTableMessages,
            variantsTableService
        });
        this._localDataService = localDataService;
        this.highlightProfile = this._localDataService.getSettings().highlightProfile;

        const globalSettingsChangedHandler = (state) => {
            this.highlightProfile = state.highlightProfile;
        };

        this.dispatcher.on('settings:change', globalSettingsChangedHandler);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
        });

        this.initEvents();
    }

    //todo doesn't need events
    //variants:loading:started and variants:loading:finished - should be promise from service
    events = {
        'activeVariants': ::this.resizeGrid,
        'display:variants:filter': ::this.refreshScope,
        'pageVariations:change': ::this.getDataOnPage,
        'reference:change': ::this.initialize,
        'variants:loading:finished': ::this.variantsLoadingFinished,
        'variants:loading:started': ::this.initialize
    };

    $onInit() {
        this.initialize();
    }

    refreshScope(needRefresh) {
        if (needRefresh) {
            this.$scope.$apply();
        }
    }

    get isProjectSelected() {
        return this.projectContext.reference;
    }

    async initialize() {
        this.errorMessageList = [];
        if (this.isProjectSelected) {
            this.isProgressShown = true;
            this.variantsLoadError = null;
            Object.assign(this.gridOptions, {
                appScopeProvider: this.$scope,
                columnDefs: this.variantsTableService.getVariantsGridColumns([], []),
                onRegisterApi: (gridApi) => {
                    this.gridApi = gridApi;
                    this.gridApi.core.handleWindowResize();
                    this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                    this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                    this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                    this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, ::this.getDataDown);
                    this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, ::this.getDataUp);
                    this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
                    this.gridApi.core.on.scrollEnd(this.$scope, ::this.changeCurrentPage);
                },
                rowTemplate: require('./ngbVariantsTable_row.tpl.html')
            });
            await this.loadData();
        } else {
            this.variantsLoadError = null;
            this.isProgressShown = false;
            this.gridOptions.columnDefs = [];
        }
    }

    async loadData() {
        try {
            if (!this.projectContext.reference) {
                this.isProgressShown = false;
                this.variantsLoadError = null;
                this.$timeout(this.$scope.$apply());
                return;
            }
            if (this.projectContext.containsVcfFiles) {
                if (this.projectContext.filteredVariants.length || this.projectContext.variantsPageError) {
                    this.variantsLoadingFinished();
                }
            } else {
                this.onError(this.variantsTableMessages.ErrorMessage.VcfNotFound);
                this.isProgressShown = false;
                this.variantsLoadError = null;
            }
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(::this.$scope.$apply);
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

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

    rowClick(row) {
        const entity = row.entity;
        const chromosomeName = `${entity.chrName}`.toLowerCase();
        const chromosome = this.projectContext.currentChromosome ?
            this.projectContext.currentChromosome.name : null;

        if (chromosome !== chromosomeName) {
            this.projectContext.changeState({
                chromosome: {
                    name: chromosomeName
                },
                viewport: {
                    end: entity.startIndex,
                    start: entity.startIndex
                }
            });
        } else {
            this.projectContext.changeState({
                viewport: {
                    end: entity.startIndex,
                    start: entity.startIndex
                }
            });
        }
    }

    showInfo(entity, event) {
        // TODO: manage variants from tracks opened by url
        const state = new EventVariationInfo(
            {
                chromosome: entity.chromosome,
                id: entity.variantId,
                position: entity.startIndex,
                projectId: entity.projectId,
                projectIdNumber: entity.projectIdNumber,
                type: entity.variationType,
                vcfFileId: entity.vcfFileId
            }
        );
        this.dispatcher.emitSimpleEvent('variant:details:select', {variant: state});
        event.stopImmediatePropagation();
    }

    variantsLoadingStarted() {

    }

    variantsLoadingFinished() {
        if (!this.projectContext.reference) {
            this.gridOptions.columnDefs = [];
            return;
        }
        if (this.projectContext.variantsPageError) {
            this.variantsLoadError = this.projectContext.variantsPageError;
            this.gridOptions.data = [];
        } else {
            this.variantsLoadError = null;
            this.gridOptions.columnDefs = this.variantsTableService.getVariantsGridColumns();
            this.gridOptions.data = this.highlightRows(this.projectContext.filteredVariants);
        }
        this.isProgressShown = this.projectContext.isVariantsLoading;

        this.$timeout(::this.$scope.$apply);
    }

    getDataDown() {
        if (this.projectContext.lastPageVariations === this.projectContext.totalPagesCountVariations) return;

        this.projectContext.lastPageVariations++;

        this.projectContext.loadVariations(this.projectContext.lastPageVariations).then((data) => {
            if (!this.isProjectSelected) {
                return;
            }
            if (this.projectContext.variantsPageError) {
                this.variantsLoadError = this.projectContext.variantsPageError;
                this.gridOptions.data = [];
            } else {
                this.variantsLoadError = null;
                this.gridApi.infiniteScroll.saveScrollPercentage();
                this.gridOptions.data = this.gridOptions.data.concat(this.highlightRows(data));
                this.gridApi.infiniteScroll.dataLoaded(
                    this.projectContext.firstPageVariations > 1,
                    (this.projectContext.totalPagesCountVariations === undefined && this.projectContext.hasMoreVariations)
                    || this.projectContext.lastPageVariations < this.projectContext.totalPagesCountVariations);
            }
        });
    }

    getDataUp() {
        if (this.projectContext.firstPageVariations === 1) return;

        this.projectContext.firstPageVariations--;

        this.projectContext.loadVariations(this.projectContext.firstPageVariations).then((data) => {
            if (this.projectContext.variantsPageError) {
                this.variantsLoadError = this.projectContext.variantsPageError;
                this.gridOptions.data = [];
            } else {
                this.variantsLoadError = null;
                this.gridApi.infiniteScroll.saveScrollPercentage();
                this.gridOptions.data = this.highlightRows(data).concat(this.gridOptions.data);
                const self = this;
                this.$timeout(function () {
                    self.gridApi.infiniteScroll.dataLoaded(
                        self.projectContext.firstPageVariations > 1,
                        (self.projectContext.totalPagesCountVariations === undefined && self.projectContext.hasMoreVariations)
                        || self.projectContext.lastPageVariations < self.projectContext.totalPagesCountVariations);
                });
            }
        });
    }

    getDataOnPage(page) {
        this.projectContext.firstPageVariations = page;
        this.projectContext.lastPageVariations = page;
        this.projectContext.currentPageVariations = page;

        this.gridApi.infiniteScroll.setScrollDirections(false, false);
        this.gridOptions.data = [];
        this.projectContext.loadVariations(page).then((data) => {
            if (this.projectContext.variantsPageError) {
                this.variantsLoadError = this.projectContext.variantsPageError;
                this.gridOptions.data = [];
            } else {
                this.variantsLoadError = null;
                const self = this;
                this.gridOptions.data = this.highlightRows(data);
                this.$timeout(function () {
                    self.gridApi.infiniteScroll.resetScroll(
                        self.projectContext.firstPageVariations > 1,
                        (self.projectContext.totalPagesCountVariations === undefined && self.projectContext.hasMoreVariations)
                        || self.projectContext.lastPageVariations < self.projectContext.totalPagesCountVariations);
                });
            }
        });
    }


    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.projectContext.orderByVariations = sortColumns.map(sc => ({
                desc: sc.sort.direction === 'desc',
                field: this.projectContext.orderByColumnsVariations[sc.field] || sc.field
            }));
        } else {
            this.projectContext.orderByVariations = null;
        }

        this.projectContext.firstPageVariations = 1;
        this.projectContext.lastPageVariations = 1;

        this.gridApi.infiniteScroll.setScrollDirections(false, false);
        this.gridOptions.data = [];
        this.projectContext.loadVariations(1).then((data) => {
            if (this.projectContext.variantsPageError) {
                this.variantsLoadError = this.projectContext.variantsPageError;
                this.gridOptions.data = [];
            } else {
                this.variantsLoadError = null;
                const self = this;
                this.gridOptions.data = this.highlightRows(data);
                this.$timeout(function () {
                    self.gridApi.infiniteScroll.resetScroll(
                        self.projectContext.firstPageVariations > 1,
                        (self.projectContext.totalPagesCountVariations === undefined && self.projectContext.hasMoreVariations)
                        || self.projectContext.lastPageVariations < self.projectContext.totalPagesCountVariations);
                });
            }
        });
    }

    changeCurrentPage(row) {
        this.$timeout(() => {
            if (row.newScrollTop) {
                const sizePage = this.projectContext.variationsPageSize * ROW_HEIGHT;
                const currentPageVariations = Math.round(this.projectContext.firstPageVariations + row.newScrollTop / sizePage);
                if (this.projectContext.currentPageVariations !== currentPageVariations) {
                    this.projectContext.currentPageVariations = currentPageVariations;
                    this.dispatcher.emit('pageVariations:scroll', this.projectContext.currentPageVariations);
                }
            }
        });
    }

    resizeGrid() {
        if (this.gridApi) {
            this.gridApi.infiniteScroll.setScrollDirections(false, false);
            this.gridApi.infiniteScroll.saveScrollPercentage();
            this.gridApi.core.handleWindowResize();

            const self = this;
            this.$timeout(function () {
                self.gridApi.infiniteScroll.dataLoaded(
                    self.projectContext.firstPageVariations > 1,
                    (self.projectContext.totalPagesCountVariations === undefined && self.projectContext.hasMoreVariations)
                    || self.projectContext.lastPageVariations < self.projectContext.totalPagesCountVariations);
            });
        }
    }

    highlightRows(data) {
        const result = [];
        // TODO: remove after settings injection
        this.highlightProfile = {
            'is_default': true,
            'conditions': [
                {
                    'highlight_color': 'ffbdbd',
                    'condition': 'variationType == DEL'
                },
                {
                    'highlight_color': 'ffff00',
                    'condition': 'variationType == INS'
                }
            ]
        };
        const parsedHighlightProfile = this.highlightProfile.conditions.map(item => ({
            highlightColor: item.highlight_color,
            parsedCondition: VcfHighlightConditionService.parseFullCondition(item.condition)
        }));
        data.forEach(row => {
            parsedHighlightProfile.forEach(item => {
                if (VcfHighlightConditionService.isHighlighted(row, item.parsedCondition)) {
                    row.highlightColor = `#${item.highlightColor}`;
                }
                result.push(row);
            });
        });
        return result;
    }
}
