import {EventGeneInfo} from '../../../shared/utils/events';
import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbGenesTableController extends baseController {
    dispatcher;
    projectContext;
    isProgressShown = true;
    isEmptyResult = false;
    errorMessageList = [];
    geneLoadError = null;
    displayGenesFilter = false;
    geneTypeColor = {};
    gridOptions = {
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        enableInfiniteScroll: true,
        headerRowHeight: 20,
        height: '100%',
        infiniteScrollDown: true,
        infiniteScrollRowsFromEnd: 10,
        infiniteScrollUp: false,
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
        useExternalSorting: true
    };
    viewDataLength;
    maxViewDataLength;
    events = {
        'genes:refresh': this.reloadGenes.bind(this),
        'display:genes:filter': this.refreshScope.bind(this),
        'reference:change': this.initialize.bind(this),
        'genes:values:loaded': this.initialize.bind(this),
        'genes:restore': this.restoreState.bind(this),
    };

    constructor(
        $scope,
        $timeout,
        dispatcher,
        appLayout,
        ngbGenesTableService,
        uiGridConstants,
        projectContext
    ) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            appLayout,
            ngbGenesTableService,
            uiGridConstants,
            projectContext
        });
        this.getStyle = this.ngbGenesTableService.getStyle(this.ngbGenesTableService);
        this.displayGenesFilter = this.ngbGenesTableService.displayGenesFilter;
        this.initEvents();
    }

    getColor = () => {};

    static get UID() {
        return 'ngbGenesTableController';
    }

    $onInit() {
        this.initialize();
    }

    refreshScope(needRefresh) {
        this.displayGenesFilter = this.ngbGenesTableService.displayGenesFilter;
        if (needRefresh) {
            this.$scope.$apply();
        }
    }

    async initialize() {
        this.errorMessageList = [];
        this.geneLoadError = null;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbGenesTableService.getGenesGridColumns(),
            paginationPageSize: this.ngbGenesTableService.genesPageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, this.getDataDown.bind(this));
                this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, this.getDataUp.bind(this));
            }
        });
        this.reloadGenes();
    }

    async reloadGenes() {
        this.isProgressShown = true;
        this.errorMessageList = [];
        this.geneLoadError = undefined;
        this.isEmptyResults = false;
        this.gridOptions.data = [];
        this.ngbGenesTableService.resetPagination();
        if (this.gridApi) {
            this.gridApi.infiniteScroll.setScrollDirections(false, false);
            this.$timeout(() => {
                this.gridApi.infiniteScroll.resetScroll(false, false);
            });
        }
        return this.getDataDown();
    }

    async getDataDown() {
        return this.appendData(false);
    }

    async getDataUp() {
        return this.appendData(true);
    }

    async appendData(isScrollTop) {
        if (!this.projectContext.reference) {
            return;
        }
        try {
            const data = await this.ngbGenesTableService.loadGenes(
                this.projectContext.reference.id,
                isScrollTop
            );
            if (this.ngbGenesTableService.genesTableError) {
                this.geneLoadError = this.ngbGenesTableService.genesTableError;
                this.gridOptions.data = [];
                this.isEmptyResults = false;
            } else if (data.length) {
                this.geneLoadError = null;
                this.gridOptions.columnDefs = this.ngbGenesTableService.getGenesGridColumns();
                this.isEmptyResults = false;
                let viewData;
                this.maxViewDataLength = this.ngbGenesTableService.genesPageSize * (this.ngbGenesTableService.maxVisiblePages - 1)
                    + this.ngbGenesTableService.lastPageLength;
                if (isScrollTop) {
                    viewData = data.concat(this.gridOptions.data);
                    this.ngbGenesTableService.firstPage -= 1;
                } else {
                    viewData = this.gridOptions.data.concat(data);
                    this.ngbGenesTableService.lastPage += 1;
                }
                this.viewDataLength = viewData.length;
                if (this.gridApi) {
                    this.gridApi.infiniteScroll.saveScrollPercentage();
                }
                this.gridOptions.data = viewData;
                if (!this.defaultState && this.gridApi) {
                    this.defaultState = this.gridApi.saveState.save();
                }
            } else if (!this.gridOptions.data.length) {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
            if (this.gridApi) {
                return this.gridApi.infiniteScroll.dataLoaded(
                    this.ngbGenesTableService.firstPage > 0,
                    this.ngbGenesTableService.hasMoreData
                ).then(() => {
                    if (this.viewDataLength > this.maxViewDataLength) {
                        this.gridApi.infiniteScroll.saveScrollPercentage();
                        if (isScrollTop) {
                            this.gridOptions.data = this.gridOptions.data.slice(0, this.maxViewDataLength);
                            this.ngbGenesTableService.lastPage -= 1;
                            this.$timeout(() => {
                                this.gridApi.core.scrollTo(
                                    this.gridOptions.data[this.ngbGenesTableService.lastPageLength],
                                    this.gridOptions.columnDefs[0]
                                );
                            });
                        } else {
                            this.gridOptions.data = this.gridOptions.data.slice(-this.maxViewDataLength);
                            this.ngbGenesTableService.firstPage += 1;
                            this.$timeout(() => {
                                this.gridApi.infiniteScroll.dataRemovedTop(
                                    this.ngbGenesTableService.firstPage > 0,
                                    this.ngbGenesTableService.hasMoreData
                                );
                            });
                        }
                    }
                    // this.$scope.$apply();
                });
            }
        } catch (errorObj) {
            this.isProgressShown = false;
            this.onError(errorObj.message);
            this.$timeout(::this.$scope.$apply);
            return this.gridApi.infiniteScroll.dataLoaded();
        }
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    rowClick(row, event) {
        const entity = row.entity;
        if (entity) {
            const {
                startIndex,
                endIndex,
                chromosome
            } = entity;
            if (chromosome && chromosome.id && startIndex && endIndex) {
                const range = Math.abs(endIndex - startIndex);
                const start = Math.min(startIndex, endIndex) - range / 10.0;
                const end = Math.max(startIndex, endIndex) + range / 10.0;
                this.projectContext.changeState({
                    chromosome,
                    viewport: {
                        start,
                        end
                    }
                });
            }
            // navigate to track
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
                (r, k) => Object.assign(r, {[o[k]]: k}), {}
            )
        )(this.ngbGenesTableService.genesColumnTitleMap);
        const mapNameToField = function ({name}) {
            return fieldTitleMap[name] || name;
        };
        const orders = columns.map(mapNameToField);
        const r = [];
        const names = this.ngbGenesTableService.genesTableColumns;
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
        this.ngbGenesTableService.genesTableColumns = result;
    }

    sortChanged(grid, sortColumns) {
        if (!this.gridApi) {
            return;
        }
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.ngbGenesTableService.orderByGenes = sortColumns.map(sc => ({
                ascending: sc.sort.direction === 'asc',
                field: this.ngbGenesTableService.orderByColumnsGenes[sc.field] || sc.field
            }));
        } else {
            this.ngbGenesTableService.orderByGenes = null;
        }

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
        this.reloadGenes();
    }

    showInfo(entity, event) {
        const data = {
            projectId: undefined,
            chromosomeId: entity.chromosome ? entity.chromosome.id : undefined,
            startIndex: entity.startIndex,
            endIndex: entity.endIndex,
            name: entity.featureName,
            geneId: entity.featureId,
            properties: [
                entity.featureName && ['Gene', entity.featureName],
                entity.featureId && ['Gene Id', entity.featureId],
                ['Start', entity.startIndex],
                ['End', entity.endIndex],
                entity.chromosome && ['Chromosome', entity.chromosome.name]
            ].filter(Boolean),
            referenceId: entity.referenceId,
            title: entity.featureType
        };
        this.dispatcher.emitSimpleEvent('feature:info:select', data);
        event.stopImmediatePropagation();
    }

    openMolecularView(entity, event) {
        const layoutChange = this.appLayout.Panels.molecularViewer;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
        const data = new EventGeneInfo({
            startIndex: entity.start,
            endIndex: entity.end,
            geneId: entity.gene_id,
            highlight: false,
            transcriptId: entity.transcript_id
        });
        this.dispatcher.emitSimpleEvent('miew:show:structure', data);
        event.stopImmediatePropagation();
    }

    restoreState() {
        this.ngbGenesTableService.genesTableColumns = [];
        this.ngbGenesTableService.orderByGenes = null;
        this.ngbGenesTableService.resetGenesFilter();
        this.ngbGenesTableService.setDisplayGenesFilter(false, false);
        if (!this.gridApi) {
            return;
        }
        this.gridApi.saveState.restore(this.$scope, this.defaultState);
    }
}
