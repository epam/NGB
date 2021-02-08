import baseController from '../../../shared/baseController';

const ROW_HEIGHT = 35;

export default class ngbBlatSearchController extends baseController {

    static get UID() {
        return 'ngbBlatSearchController';
    }


    projectContext;
    blatSearchMessages;

    isProgressShown = true;
    errorMessageList = [];
    blatSearchEmptyResult = null;

    readSequence = null;

    gridOptions = {
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
        saveSort: true,
        saveFilter: false,
        savePinning: true,
        saveGrouping: false,
        saveGroupingExpandedStates: false,
        saveTreeView: false,
        saveSelection: false,
    };

    constructor($scope, $timeout, blatSearchMessages, blatSearchService, dispatcher, projectContext) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            blatSearchMessages,
            blatSearchService,
        });

        this.initEvents();
    }

    events = {
        'reference:change': this.initialize.bind(this),
        'read:show:blat': this.initialize.bind(this),
    };

    async $onInit() {
        await this.initialize();
    }

    get isReadSelected() {
        return !!this.blatSearchService.blatRequest;
    }

    async initialize() {
        this.errorMessageList = [];
        if (this.isReadSelected) {
            this.isProgressShown = true;
            this.blatSearchEmptyResult = null;
            Object.assign(this.gridOptions, {
                appScopeProvider: this.$scope,
                columnDefs: this.blatSearchService.getBlatSearchGridColumns([], []),
                onRegisterApi: (gridApi) => {
                    this.gridApi = gridApi;
                    this.gridApi.core.handleWindowResize();
                    this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                    this.gridApi.selection.on.rowSelectionChanged(this.$scope, this.rowClick.bind(this));
                    this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                    this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                }
            });
            await this.loadData();

            this.$timeout(this.$scope.$apply());
        } else {
            this.blatSearchEmptyResult = null;
            this.isProgressShown = false;
            this.gridOptions.columnDefs = [];

            this.$timeout(this.$scope.$apply());
        }
    }

    async loadData() {
        try {
            if (!this.projectContext.reference) {
                this.isProgressShown = false;
                this.blatSearchEmptyResult = null;
                this.gridOptions.columnDefs = [];
                return;
            }
            await this.blatSearchLoadingFinished();
        }  catch (errorObj) {
                this.onError(errorObj.message);
                this.isProgressShown = false;
        }
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    async blatSearchLoadingFinished() {
        this.blatSearchEmptyResult = null;
        this.gridOptions.columnDefs = this.blatSearchService.getBlatSearchGridColumns();
        this.gridOptions.data = await this.blatSearchService.getBlatSearchResults();
        this.readSequence = this.blatSearchService.readSequence;

        if (this.gridOptions.data && this.gridOptions.data.length) {
            this.blatSearchEmptyResult = null;
        } else {
            this.blatSearchEmptyResult = this.blatSearchMessages.ErrorMessage.EmptySearchResults;
        }

        this.isProgressShown = false;
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
        const blatRegion = {
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
                blatRegion,
            });
        } else {
            this.projectContext.changeState({ viewport, blatRegion });
        }
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
        if (sortColumns && sortColumns.length > 0) {
            this.blatSearchService.orderBy = sortColumns.map((sc) => {
                return {
                    field: sc.field,
                    desc: sc.sort.direction === 'desc',
                };
            });
        } else {
            this.blatSearchService.orderBy = null;
        }
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
        const names = this.blatSearchService.blatColumns;
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
        this.blatSearchService.blatColumns = result;
    }
}
