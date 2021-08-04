import {Debounce} from '../../../shared/utils/debounce';
import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;
const RELOAD_GENES_DELAY = 300;

export default class ngbGenesTableController extends baseController {
    dispatcher;
    projectContext;
    isProgressShown = true;
    isInitialized = false;
    reloading = false;
    isEmptyResult = false;
    errorMessageList = [];
    geneLoadError = null;
    displayGenesFilter = false;
    oldReferenceId;
    debounce = (new Debounce()).debounce;
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
        'genes:refresh': this.loadGenesWithProgress.bind(this),
        'display:genes:filter': this.refreshScope.bind(this),
        'reference:change': () => {
            this.isInitialized = false;
            if (this.projectContext.reference) {
                this.isProgressShown = true;
            }
        },
        'genes:values:loaded': this.initialize.bind(this),
        'gene:files:changed': this.loadGenesWithProgress.bind(this),
        'genes:restore': this.restoreState.bind(this)
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
        this.dispatcher.on('feature:info:saved', this.reloadGenes.bind(this));
    }

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
            rowTemplate: require('./ngbGenesTable_row.tpl.html'),
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
        this.debounce(this, this.reloadGenes.bind(this), RELOAD_GENES_DELAY)();
    }

    async loadGenesWithProgress() {
        this.isInitialized = false;
        if (this.projectContext.reference) {
            this.isProgressShown = true;
            this.debounce(this, this.reloadGenes.bind(this), RELOAD_GENES_DELAY)();
        } else {
            this.isProgressShown = false;
        }
    }

    async reloadGenes() {
        this.errorMessageList = [];
        this.geneLoadError = undefined;
        this.isEmptyResults = false;
        this.ngbGenesTableService.resetPagination();
        if (this.gridApi) {
            this.gridApi.infiniteScroll.setScrollDirections(false, false);
            this.gridApi.core.scrollTo(
                this.gridOptions.data[0],
                this.gridOptions.columnDefs[0]
            );
        }
        if (!this.projectContext.reference) {
            this.isProgressShown = false;
        } else {
            this.oldReferenceId = this.projectContext.reference.id;
            this.isInitialized = true;
            return this.getDataDown(true);
        }
    }

    async getDataDown(isReload) {
        return this.appendData(false, isReload);
    }

    async getDataUp() {
        return this.appendData(true);
    }

    async appendData(isScrollTop, isReload) {
        try {
            if (isReload) {
                this.reloading = true;
            }
            const data = await this.ngbGenesTableService.loadGenes(
                this.projectContext.reference.id,
                isScrollTop
            );
            if (isReload) {
                this.gridOptions.data = [];
                this.reloading = false;
            }
            if (this.ngbGenesTableService.genesTableError) {
                this.geneLoadError = this.ngbGenesTableService.genesTableError;
                this.gridOptions.data = [];
                this.isEmptyResults = false;
                this.isProgressShown = false;
                this.isInitialized = true;
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
                this.gridOptions.data = viewData;
                if (!this.defaultState && this.gridApi) {
                    this.defaultState = this.gridApi.saveState.save();
                }
            } else if (!this.gridOptions.data.length) {
                this.isEmptyResults = true;
                this.isProgressShown = false;
                this.isInitialized = true;
            }
            if (this.isInitialized) {
                this.isProgressShown = false;
            }
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
                });
            }
            this.$timeout(this.$scope.$apply);
        } catch (errorObj) {
            this.isProgressShown = false;
            this.isInitialized = true;
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
            const chromosomeObj = entity.chromosomeObj,
                endIndex = entity[`${this.ngbGenesTableService.defaultPrefix}endIndex`],
                startIndex = entity[`${this.ngbGenesTableService.defaultPrefix}startIndex`];
            if (chromosomeObj && chromosomeObj.id && startIndex && endIndex) {
                const range = Math.abs(endIndex - startIndex);
                const start = Math.min(startIndex, endIndex) - range / 10.0;
                const end = Math.max(startIndex, endIndex) + range / 10.0;
                this.projectContext.changeState({
                    chromosome: chromosomeObj,
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
        entity.isInfoLoading = true;
        const defaultPrefix = this.ngbGenesTableService.defaultPrefix;
        this.ngbGenesTableService.getGeneInfo(
            entity[`${defaultPrefix}featureFileId`],
            entity.uuid
        ).then(
            data => {
                delete entity.error;
                const extractProperties = (o, except = []) => Object
                    .entries(o || {})
                    .map(([key, value]) => {
                        if (
                            o.hasOwnProperty(key) &&
                            typeof o[key] !== 'object' &&
                            except.indexOf(key) === -1 &&
                            o[key] !== undefined
                        ) {
                            return [key, value, false];
                        }
                        return undefined;
                    })
                    .filter(Boolean);
                const result = {
                    projectId: undefined,
                    chromosomeId: entity[`${defaultPrefix}chromosome`],
                    startIndex: data.startIndex,
                    endIndex: data.endIndex,
                    name: data.featureName,
                    geneId: data.featureId,
                    editable: true,
                    properties: [
                        ['start', data.startIndex, false],
                        ['end', data.endIndex, false],
                        ['chromosome', entity.chromosomeObj.name, false],
                        ...extractProperties(data, [
                            'start',
                            'end',
                            'chromosome',
                            'startIndex',
                            'endIndex'
                        ]),
                        ...Object
                            .entries(data.attributes || {})
                            .map(([key, value]) => ([
                                key, value, true
                            ]))
                    ],
                    referenceId: entity.referenceId,
                    title: entity.feature,
                    fileId: entity[`${defaultPrefix}featureFileId`],
                    feature: data,
                    uuid: entity.uuid
                };
                this.dispatcher.emitSimpleEvent('feature:info:select', result);
                entity.isInfoLoading = false;
            },
            errorObj => {
                entity.error = errorObj ? errorObj.error : 'Network error';
                entity.isInfoLoading = false;
                this.$timeout(this.$scope.$apply);
            }
        );
        event.stopImmediatePropagation();
    }

    restoreState() {
        this.ngbGenesTableService.genesTableColumns = [];
        this.ngbGenesTableService.orderByGenes = null;
        this.ngbGenesTableService.resetGenesFilter();
        this.ngbGenesTableService.setDisplayGenesFilter(false, false);
        if (!this.gridApi || !this.defaultState) {
            return;
        }
        this.gridApi.saveState.restore(this.$scope, this.defaultState);
    }
}
