import baseController from '../../shared/baseController';

const ROW_HEIGHT = 35;
const PAGE_SIZE = 50;
const FIRST_PAGE = 1;

const DISPLAYED_PAGES_COUNT = 3;

export default class ngbBlastSearchPanelController extends baseController {

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    projectContext;
    blastSearchMessages;
    readSequence = null;
    isProgressShown = true;
    errorMessageList = [];
    blastSearchEmptyResult = null;
    _sequence = '';
    data = null;
    dataLength;
    blastPageSize = PAGE_SIZE;
    currentPageBlast = 0;
    prevScrollSize = 0;

    gridOptions = {
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 21,
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
        treeRowHeaderAlwaysVisible: false,
    };

    constructor(
        $scope,
        $timeout,
        blastSearchMessages,
        dispatcher,
        ngbBlastSearchService,
        projectContext
    ) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbBlastSearchService,
            projectContext,
            blastSearchMessages,
        });
        this.initEvents();
    }

    events = {
        'reference:change': ::this.initialize,
        'read:show:blast': ::this.initialize,
        'pageBlast:change': ::this.getPage,
    };

    async $onInit() {
        await this.initialize();
    }

    get isReadSelected() {
        return !!this.ngbBlastSearchService.blastRequest;
    }

    async initialize() {
        this._sequence = '';
        this.errorMessageList = [];
        this.data = null;
        this.prevScrollSize = 0;
        if (this.isReadSelected) {
            this.isProgressShown = true;
            this.blastSearchEmptyResult = null;
            Object.assign(this.gridOptions, {
                appScopeProvider: this.$scope,
                columnDefs: this.ngbBlastSearchService.getBlastSearchGridColumns([], []),
                onRegisterApi: (gridApi) => {
                    this.gridApi = gridApi;
                    this.gridApi.core.handleWindowResize();
                    this.gridApi.core.on.sortChanged(this.$scope, ::this.sortChanged);
                    this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                    this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                    this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                    // this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, ::this.getPage);
                    // this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, ::this.getPage);
                    // this.gridApi.core.on.scrollEnd(this.$scope, ::this.changeCurrentPage);
                    this.gridApi.infiniteScroll.on.needLoadMoreData(this.$scope, this.loadDataDown.bind(this));
                    this.gridApi.infiniteScroll.on.needLoadMoreDataTop(this.$scope, this.loadDataUp.bind(this));
                },
            });
            await this.loadData();
            this.$timeout(this.$scope.$apply());
        } else {
            this.blastSearchEmptyResult = null;
            this.isProgressShown = false;
            this.gridOptions.columnDefs = [];
            this.$timeout(this.$scope.$apply());
        }
    }

    getDisplayedPagesRangeMiddlePage (centerPage) {
        // correctPage(page) corrects passed `page` to be in real pages range (0 ... last page)
        const correctPage = aPage => Math.max(
            0,
            Math.min(
                aPage,
                this.totalPagesCountBlast
            )
        );
        const firstDisplayedPage = correctPage(Math.ceil(centerPage - DISPLAYED_PAGES_COUNT / 2.0));
        const lastDisplayedPage = correctPage(Math.floor(centerPage + DISPLAYED_PAGES_COUNT / 2.0));
        return {
            first: firstDisplayedPage,
            last: lastDisplayedPage
        };
    }

    loadDataDown () {
        const {last: lastPage} = this.getDisplayedPagesRangeMiddlePage(this.currentPageBlast);
        const currentElementIndex = (lastPage + 1) * PAGE_SIZE - 1;
        this.loadDataPage(
            lastPage + 1,
            {
                atBottom: true,
                currentElementIndex
            }
        );
    }

    loadDataUp () {
        const {first: firstPage} = this.getDisplayedPagesRangeMiddlePage(this.currentPageBlast);
        const currentElementIndex = firstPage * PAGE_SIZE;
        this.loadDataPage(
            firstPage - 1,
            {
                atBottom: false,
                currentElementIndex
            }
        );
    }

    /**
     * Loads data by page
     * @param page {number}
     * @param scrollingOptions {{atBottom: boolean, currentElementIndex: number}}
     */
    loadDataPage (page, scrollingOptions = {}) {
        const {
            first: firstDisplayedPage,
            last: lastDisplayedPage
        } = this.getDisplayedPagesRangeMiddlePage(page);
        if (page !== this.currentPageBlast) {
            // loading pages `firstDisplayedPage ... lastDisplayedPage`
            const firstRow = firstDisplayedPage * PAGE_SIZE;
            const lastRow = (lastDisplayedPage + 1) * PAGE_SIZE - 1;
            const newData = this.data.slice(firstRow, lastRow + 1);
            console.log(`load pages ${firstDisplayedPage}...${lastDisplayedPage} (items #${firstRow}...#${lastRow}):`, newData);
            if (scrollingOptions) {
                const {
                    atBottom,
                    currentElementIndex
                } = scrollingOptions;
                const correctIndex = aIndex => Math.max(firstRow, Math.min(lastRow, aIndex));
                const visibleRowsCount = 0; // todo
                const relativeIndex = correctIndex(currentElementIndex - (atBottom ? 0 : visibleRowsCount)) - firstRow;
                const dataElement = newData[relativeIndex];
                console.log(`scrolling element #${currentElementIndex} to be visible at ${atBottom ? 'bottom' : 'top'}`, dataElement);
            } else {
                // todo: scroll to first element on `page`
            }
            // todo: update currentPage
        }
    }

    async loadData() {
        try {
            if (!this.projectContext.reference) {
                this.isProgressShown = false;
                this.blastSearchEmptyResult = null;
                this.gridOptions.columnDefs = [];
                return;
            }
            await this.blastSearchLoadingFinished();
        }  catch (errorObj) {
            this.onError(errorObj.message);
            this.isProgressShown = false;
        }
    }

    handleOpenGenomeView() {
        const data = this.data;
        this.dispatcher.emitSimpleEvent('blast:whole:genome:view', { data });
    }

    handleSearchGenome(sequence) {
        if (sequence !== this.readSequence) {
            this.blastSearchLoadingFinished();
        }
    }

    get sequence() {
        return this._sequence;
    }

    set sequence(sequence) {
        this._sequence = sequence;
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.ngbBlastSearchService.orderBy = sortColumns.map(sc =>
                ({
                    field: sc.field,
                    desc: sc.sort.direction === 'desc',
                })
            );
        } else {
            this.ngbBlastSearchService.orderBy = null;
        }
        this.dispatcher.emit('pageBlast:scroll', 1);
        this.getPage([1, 0]);
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const { columns } = this.gridApi.saveState.save();
        const mapNameToField = function ({name}) {
            return (name.charAt(0).toLowerCase() + name.slice(1)).replace(/[\s\n\t]/g, '');
        };
        const orders = columns.map(mapNameToField);
        const r = [];
        const names = this.ngbBlastSearchService.blastColumns;
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
        this.ngbBlastSearchService.blastColumns = result;
    }

    rowClick(row) {
        const entity = row.entity;
        const chromosome = this.currentChromosome
            ? this.currentChromosome.name
            : null;

        let chromosomeName;
        if (chromosome && !chromosome.toLowerCase().includes('chr')) {
            chromosomeName = `${entity.chr.slice(3)}`.toLowerCase();
        } else {
            chromosomeName = entity.chr;
        }

        const addition = (entity.endIndex - entity.startIndex) * 0.1;
        const viewport = {
            start: entity.startIndex - addition,
            end: entity.endIndex + addition,
        };
        const blastRegion = {
            start: entity.startIndex,
            end: entity.endIndex,
            chromosomeName,
        };

        if (chromosome !== chromosomeName) {
            this.projectContext.changeState({
                chromosome: {
                    name: chromosomeName,
                },
                viewport,
                blastRegion,
            });
        } else {
            this.projectContext.changeState({ viewport, blastRegion });
        }
    }

    getPage([page, scrollLastRow]) {
        if (this.currentPageBlast !== page) {
            this.currentPageBlast = page;
            this.gridOptions.data = [];

            const firstRow = Math.max((this.blastPageSize * (page - 2)), 0);
            const lastRow = Math.min((this.blastPageSize * (page + 1)), this.gridOptions.totalItems);

            this.gridApi.infiniteScroll.setScrollDirections(false, false);
            this.gridOptions.data = this.data.slice(firstRow, lastRow);
        }
        this.$timeout(() => {
            this.gridApi.infiniteScroll.resetScroll();
            if (page !== 1) {
                if (scrollLastRow === 0) {
                    this.prevScrollSize = 0;
                    const gridHeight = this.gridApi.grid.gridHeight - this.gridOptions.headerRowHeight;
                    scrollLastRow = Math.floor(gridHeight / ROW_HEIGHT + this.blastPageSize);
                    this.$timeout(() => {this.gridApi.core.scrollTo(this.gridOptions.data[scrollLastRow], this.gridOptions.columnDefs[0]);});
                } else {
                    this.$timeout(() => {this.gridApi.core.scrollTo(this.gridOptions.data[scrollLastRow], this.gridOptions.columnDefs[0]);});
                }
            } else if (page === 1 && scrollLastRow) {
                this.$timeout(() => {this.gridApi.core.scrollTo(this.gridOptions.data[scrollLastRow], this.gridOptions.columnDefs[0]);});
            }
        });
    }

    changeCurrentPage(row) {
        const sizePage = this.blastPageSize * ROW_HEIGHT;
        if (row.newScrollTop) {
            const scrollSize = row.newScrollTop / sizePage;
            const pageEnd = this.currentPageBlast === 1 ? 1 : 2;
            if (
                scrollSize > pageEnd &&
                this.currentPageBlast < this.totalPagesCountBlast
            ) {
                const gridHeight = this.gridApi.grid.gridHeight - this.gridOptions.headerRowHeight;
                const firstPage = this.currentPageBlast === 1 ? 0 : this.blastPageSize;
                const scrollLastRow = Math.floor(
                    (row.newScrollTop + gridHeight) / ROW_HEIGHT - firstPage);
                const newPage = scrollSize < pageEnd + 1 ? 1 : Math.floor(scrollSize);
                this.$timeout(() => {
                    this.dispatcher.emit('pageBlast:scroll', (this.currentPageBlast + newPage));
                    this.getPage([this.currentPageBlast + newPage, scrollLastRow]);
                });
                this.prevScrollSize = row.newScrollTop;
            }
            if (
                this.prevScrollSize > row.newScrollTop
                && row.newScrollTop < sizePage
                && this.currentPageBlast > 1
            ) {
                const gridHeight = this.gridApi.grid.gridHeight - this.gridOptions.headerRowHeight;
                const pageStart = this.currentPageBlast === 2 ? 0 : 1;
                const scrollLastRow = Math.floor((sizePage  * pageStart + row.newScrollTop + gridHeight) / ROW_HEIGHT);
                this.dispatcher.emit('pageBlast:scroll', (this.currentPageBlast - 1));
                this.getPage([this.currentPageBlast - 1, scrollLastRow]);
                this.prevScrollSize = row.newScrollTop;
            }
            this.prevScrollSize = row.newScrollTop;
        }
        if (row.newScrollTop === 0 && this.prevScrollSize !== 0 && this.currentPageBlast > 1) {
            if (this.currentPageBlast > 2) {
                const scrollLastRow = this.blastPageSize * (this.currentPageBlast > 3 ? 2 : 1);
                this.dispatcher.emit('pageBlast:scroll', (this.currentPageBlast - 2));
                this.getPage([this.currentPageBlast - 2, scrollLastRow]);
            } else if (this.currentPageBlast === 2) {
                this.dispatcher.emit('pageBlast:scroll', (this.currentPageBlast - 1));
                this.getPage([this.currentPageBlast - 1, 0]);
            }
        }
    }

    async blastSearchLoadingFinished() {
        this.blastSearchEmptyResult = null;
        this.gridOptions.columnDefs = this.ngbBlastSearchService.getBlastSearchGridColumns();
        this.data = (await this.ngbBlastSearchService.getBlastSearchResults()) || [];
        this.dataLength = this.data.length;
        this.readSequence = this.ngbBlastSearchService.readSequence;

        if (!this._sequence) {
            this.sequence = this.readSequence;
        }

        if (this.data && this.dataLength) {
            this.blastSearchEmptyResult = null;
            this.gridOptions.totalItems = this.dataLength;
            this.totalPagesCountBlast = Math.ceil(this.dataLength/this.blastPageSize);
            this.dispatcher.emitSimpleEvent('blast:loading:finished', [this.totalPagesCountBlast, 1]);
            this.$timeout(() => this.getPage([1, 0]));
        } else {
            this.blastSearchEmptyResult = this.blastSearchMessages.ErrorMessage.EmptySearchResults;
        }
        this.isProgressShown = false;
        this.$timeout(::this.$scope.$apply);
    }
}
