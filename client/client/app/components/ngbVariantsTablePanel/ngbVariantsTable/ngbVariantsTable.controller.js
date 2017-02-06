import {EventVariationInfo} from '../../../shared/utils/events';
import  baseController from '../../../shared/baseController';

export default class ngbVariantsTableController extends baseController {

    static get UID() {
        return 'ngbVariantsTableController';
    }

    dispatcher;
    projectContext;
    variantsTableMessages;

    isProgressShown = true;
    errorMessageList = [];

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
        rowHeight: 48,
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
        saveSelection: false
    };


    constructor($scope, $timeout, variantsTableMessages, variantsTableService, uiGridConstants, dispatcher, projectContext) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            projectContext,
            uiGridConstants,
            variantsTableMessages,
            variantsTableService});

        this.initEvents();
    }

    //todo doesn't need events
    //variants:loading:started and variants:loading:finished - should be promise from service
    events = {
        'reference:change': ::this.initialize,
        'variants:loading:finished': ::this.variantsLoadingFinished,
        'variants:loading:started': ::this.initialize
    };

    $onInit() {
        this.initialize();
    }

    get isProjectSelected() {
        return this.projectContext.reference;
    }

    async initialize() {
        this.errorMessageList = [];
        if (this.isProjectSelected) {
            this.isProgressShown = true;
            Object.assign(this.gridOptions, {
                appScopeProvider: this.$scope,
                columnDefs: this.variantsTableService.getVariantsGridColumns([], []),
                onRegisterApi: (gridApi) => {
                    this.gridApi = gridApi;
                    this.gridApi.core.handleWindowResize();
                    this.gridApi.colMovable.on.columnPositionChanged(this.$scope, ::this.saveColumnsState);
                    this.gridApi.colResizable.on.columnSizeChanged(this.$scope, ::this.saveColumnsState);
                    this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                    this.gridApi.core.on.sortChanged(this.$scope, ::this.saveColumnsState);
                }
            });
            await this.loadData();
        } else {
            this.isProgressShown = false;
            this.gridOptions.columnDefs = [];
        }
    }

    async loadData() {
        try {
            if (!this.projectContext.reference) {
                this.isProgressShown = false;
                this.$timeout(this.$scope.$apply());
                return;
            }
            if (this.projectContext.containsVcfFiles) {
                if (this.projectContext.filteredVariants.length) {
                    this.variantsLoadingFinished();
                }
            } else {
                this.onError(this.variantsTableMessages.ErrorMessage.VcfNotFound);
                this.isProgressShown = false;
            }
        }
        catch (errorObj) {
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
        const mapNameToField = function({name}) {
            switch (name) {
                case 'Type': return 'variationType';
                case 'Chr': return 'chrName';
                case 'Gene': return 'geneNames';
                case 'Position': return 'startIndex';
                case 'Info': return 'info';
                default: return name;
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
        }
        else {
            this.projectContext.changeState({
                viewport: {
                    end: entity.startIndex,
                    start: entity.startIndex
                }
            });
        }
    }

    showInfo(entity, event) {
        const state = new EventVariationInfo(
            {
                chromosome: entity.chromosome,
                id: entity.variantId,
                position: entity.startIndex,
                type: entity.variationType,
                vcfFileId: entity.vcfFileId,
                projectId: entity.projectId
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
        this.gridOptions.columnDefs = this.variantsTableService.getVariantsGridColumns();
        this.gridOptions.data = this.projectContext.filteredVariants;
        this.isProgressShown = this.projectContext.isVariantsLoading;
        this.$timeout(::this.$scope.$apply);
    }
}
