import baseController from '../../shared/baseController';

const ROW_HEIGHT = 35;

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

    paginationOptions = {
        pageNumber: 1,
        pageSize: 10,
    };

    gridOptions = {
        enableFiltering: false,
        enableGridMenu: false,
        enableHorizontalScrollbar: 0,
        enablePinning: false,
        enableRowHeaderSelection: false,
        enableRowSelection: true,
        headerRowHeight: 21,
        height: '100%',
        multiSelect: false,
        paginationPageSize: 10,
        paginationPageSizes: [10, 20, 50, 100],
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
        showColumnFooter: true,
        showHeader: true,
        treeRowHeaderAlwaysVisible: false,
        useExternalPagination: true,
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
                    this.gridApi.pagination.on.paginationChanged(this.$scope, ::this.paginationChanged);
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

    async handleOpenGenomeView() {
        const data = this.data;
        const speciesList = (await this.ngbBlastSearchService.generateSpeciesList());
        this.dispatcher.emitSimpleEvent('blast:whole:genome:view', { data, speciesList });
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
        this.getPage();
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
        const chromosome = this.projectContext.currentChromosome
            ? this.projectContext.currentChromosome.name
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

    paginationChanged(newPage, pageSize) {
        this.paginationOptions.pageNumber = newPage;
        this.paginationOptions.pageSize = pageSize;
        this.getPage();
    }

    getPage() {
        const firstRow = (this.paginationOptions.pageNumber - 1) * this.paginationOptions.pageSize;
        this.gridOptions.data = this.data.slice(firstRow, firstRow + this.paginationOptions.pageSize);
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
            this.gridOptions.totalItems = this.dataLength;
            this.getPage();
        }

        if (this.data && this.data.length) {
            this.blastSearchEmptyResult = null;
        } else {
            this.blastSearchEmptyResult = this.blastSearchMessages.ErrorMessage.EmptySearchResults;
        }
        this.isProgressShown = false;
    }
}
