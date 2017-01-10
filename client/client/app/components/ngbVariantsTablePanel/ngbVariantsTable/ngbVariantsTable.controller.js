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
        treeRowHeaderAlwaysVisible: false
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
        'ngbColumns:change': ::this.onColumnChange,
        'projectId:change': ::this.initialize,
        'variants:loading:finished': ::this.variantsLoadingFinished,
        'variants:loading:started': ::this.variantsLoadingStarted
    };

    $onInit() {
        this.initialize();
    }

    get isProjectSelected() {
        return this.projectContext.project;
    }

    async initialize() {
        this.errorMessageList = [];
        if (this.isProjectSelected) {
            this.isProgressShown = true;
            Object.assign(this.gridOptions, {
                appScopeProvider: this.$scope,
                columnDefs: this.variantsTableService.getVariantsGridColumns(),
                onRegisterApi: (gridApi) => {
                    this.gridApi = gridApi;
                    this.gridApi.core.handleWindowResize();
                    this.gridApi.selection.on.rowSelectionChanged(this.$scope, ::this.rowClick);
                    this.gridApi.core.on.columnVisibilityChanged(this.$scope, ::this.columnChange);
                }
            });
            await this.loadData();
        } else {
            this.isProgressShown = false;
            this.gridOptions.columnDefs = [];
        }
    }

    async onColumnChange(infoFields) {
        const projectId = this.projectContext.projectId;
        if (!projectId) {
            this.gridOptions.columnDefs = [];
            return;
        }
        infoFields = infoFields || [];
        const columnList = this.projectContext.vcfInfo.filter(m => infoFields.indexOf(m.name) >= 0);
        this.gridOptions.columnDefs = this.variantsTableService.getVariantsGridColumns(columnList);
    }

    async loadData() {
        try {
            if (!this.projectContext.project) {
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

    columnChange(changedColumn) {
        const infoFields = this.projectContext.vcfInfoColumns;
        const index = infoFields.indexOf(changedColumn.name);
        if (index > -1) {
            infoFields.splice(index, 1);
        }
        this.projectContext.changeVcfInfoFields(infoFields);
    }

    onError(message) {
        this.errorMessageList.push(message);
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
                vcfFileId: entity.vcfFileId
            }
        );
        this.dispatcher.emitSimpleEvent('variant:details:select', {variant: state});
        event.stopImmediatePropagation();
    }

    variantsLoadingStarted() {

    }

    variantsLoadingFinished() {
        this.gridOptions.data = this.projectContext.filteredVariants;
        this.isProgressShown = this.projectContext.isVariantsLoading;
        this.$timeout(::this.$scope.$apply);
    }
}
