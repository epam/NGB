import baseController from '../../../../shared/baseController';
import {Debounce} from '../../../../shared/utils/debounce';

const ROW_HEIGHT = 35;
const RESIZE_DELAY = 300;

export default class ngbHomologeneResultTableController extends baseController {
    dispatcher;
    isProgressShown = true;
    isEmptyResults = false;
    errorMessageList = [];
    searchResultTableLoadError = null;
    debounce = (new Debounce()).debounce;
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
        enablePaginationControls: false,
    };
    events = {
        'homologs:homologene:result:page:change': ::this.getDataOnPage
    };

    constructor($scope, $timeout, $mdDialog, projectContext, projectDataService,
        ngbHomologeneTableService, ngbHomologeneResultService, ngbHomologsService, dispatcher) {
        super();

        Object.assign(this, {
            $scope,
            $timeout,
            $mdDialog,
            dispatcher,
            projectContext,
            projectDataService,
            ngbHomologeneTableService,
            ngbHomologeneResultService,
            ngbHomologsService
        });

        this.initEvents();
        this.$scope.$on('$destroy', () => {
            this.ngbHomologsService.isEmptyResults = true;
        });
    }

    static get UID() {
        return 'ngbHomologeneResultTableController';
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        this.errorMessageList = [];
        this.isProgressShown = true;
        this.searchResultTableLoadError = null;
        this.ngbHomologeneResultService.currentPage = 1;
        Object.assign(this.gridOptions, {
            appScopeProvider: this.$scope,
            columnDefs: this.ngbHomologeneResultService.getHomologeneResultGridColumns(),
            paginationCurrentPage: this.ngbHomologeneResultService.currentPage,
            paginationPageSize: this.ngbHomologeneResultService.pageSize,
            onRegisterApi: (gridApi) => {
                this.gridApi = gridApi;
                this.gridApi.core.handleWindowResize();
                this.gridApi.core.on.sortChanged(this.$scope, this.sortChanged.bind(this));
                this.gridApi.colMovable.on.columnPositionChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.colResizable.on.columnSizeChanged(this.$scope, this.saveColumnsState.bind(this));
                this.gridApi.core.on.gridDimensionChanged(this.$scope, this.debounce(this, this.onResize.bind(this), RESIZE_DELAY));
                this.gridApi.core.on.renderingComplete(this.$scope, gridApi => {
                    this.debounce(this, this.onResize.bind(this), RESIZE_DELAY)(0, 0, gridApi.grid.gridHeight);
                });
            }
        });
        this.loadData();
    }

    loadData() {
        try {
            const result = this.ngbHomologeneTableService.getHomologeneResultById(this.ngbHomologsService.currentHomologeneId);
            if (this.ngbHomologeneTableService.searchResultTableError) {
                this.searchResultTableLoadError = this.ngbHomologeneTableService.searchResultTableError;
                this.gridOptions.data = [];
                this.gridOptions.totalItems = 0;
                this.isEmptyResults = false;
            } else if (result.length) {
                this.searchResultTableLoadError = null;
                this.gridOptions.data = result;
                this.gridOptions.paginationPageSize = this.ngbHomologeneResultService.pageSize;
                this.gridOptions.totalItems = result.length;
                this.isEmptyResults = false;
            } else {
                this.isEmptyResults = true;
            }
            this.isProgressShown = false;
            this.ngbHomologsService.isEmptyResults = this.isEmptyResults;
        } catch (errorObj) {
            this.onError(errorObj.message);
        }
        this.$timeout(() => this.$scope.$apply());
    }

    onError(message) {
        this.errorMessageList.push(message);
    }

    getDataOnPage(page) {
        if (this.gridApi) {
            this.gridApi.pagination.seek(page);
        }
    }

    saveColumnsState() {
        if (!this.gridApi) {
            return;
        }
        const {columns} = this.gridApi.saveState.save();
        const orders = columns.map(c => c.name);
        const r = [];
        const names = this.ngbHomologeneResultService.homologeneResultColumns;
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
        this.ngbHomologeneResultService.homologeneResultColumns = result;
    }

    sortChanged(grid, sortColumns) {
        this.saveColumnsState();
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
    }

    onResize(oldGridHeight, oldGridWidth, newGridHeight) {
        const pageSize = Math.floor(newGridHeight / ROW_HEIGHT) - 1;
        if (pageSize) {
            this.ngbHomologeneResultService.pageSize = pageSize;
            this.ngbHomologeneResultService.totalPages = Math.ceil(this.gridOptions.data.length / this.ngbHomologeneResultService.pageSize);
            this.gridOptions.paginationPageSize = pageSize;
            this.$timeout(() => this.$scope.$apply());
        }
    }

    async navigateToTrack(entity) {
        const coordinates = await this.projectDataService.getFeatureCoordinates(entity.accession_id, 'PROTEIN', entity.taxId);
        if (coordinates && !coordinates.error) {
            const range = Math.abs(coordinates.end - coordinates.start);
            const start = Math.min(coordinates.start, coordinates.end) - range / 10.0;
            const end = Math.max(coordinates.start, coordinates.end) + range / 10.0;
            const switchingReference = this.projectContext.reference && this.projectContext.reference.id !== +coordinates.referenceId;

            if (switchingReference) {
                const [chosenReference] = (this.projectContext.references || []).filter(r => r.id === +coordinates.referenceId);
                if (chosenReference) {
                    const tracksOptions = {};
                    const referenceTrackState = {
                        referenceShowForwardStrand: true,
                        referenceShowReverseStrand: true,
                        referenceShowTranslation: false
                    };
                    tracksOptions.reference = chosenReference;
                    tracksOptions.shouldAddAnnotationTracks = true;
                    tracksOptions.tracks = [chosenReference].map(track => ({
                        ...track,
                        projectId: '',
                        isLocal: true
                    }));
                    tracksOptions.tracksState = [
                        {...chosenReference, state: referenceTrackState}
                    ].map(track => ({
                        bioDataItemId: track.name,
                        duplicateId: track.duplicateId,
                        projectId: '',
                        isLocal: true,
                        format: track.format,
                        state: track.state
                    }));
                    const [currentReferenceTrack] = this.projectContext.getActiveTracks()
                        .filter(track => track.format === 'REFERENCE');
                    if (currentReferenceTrack && currentReferenceTrack.instance) {
                        currentReferenceTrack.instance.state.referenceShowForwardStrand = true;
                        currentReferenceTrack.instance.state.referenceShowReverseStrand = true;
                        currentReferenceTrack.instance.reportTrackState(true);
                        currentReferenceTrack.instance.requestRender();
                    }
                    const confirm = this.$mdDialog.confirm()
                        .title(`Switch reference ${this.projectContext.reference ? this.projectContext.reference.name : ''}` +
                            ` to ${chosenReference.name}?`)
                        .textContent('All opened tracks will be closed.')
                        .ariaLabel('Change reference')
                        .ok('OK')
                        .cancel('Cancel');

                    this.$mdDialog.show(confirm)
                        .then(() => this.projectContext.changeState({
                            reference: chosenReference,
                            chromosome: {id: coordinates.chromosomeId},
                            viewport: {
                                start,
                                end
                            },
                            ...tracksOptions
                        }));
                }
            } else {
                this.projectContext.changeState({
                    chromosome: {id: coordinates.chromosomeId},
                    viewport: {
                        start,
                        end
                    }
                });
            }
            // navigate to track
        } else {
            event.stopImmediatePropagation();
            window.open(`https://www.ncbi.nlm.nih.gov/gene/${entity.geneId}`);
            return false;
        }
    }
}
