import baseController from '../../shared/baseController';
import ngbDataSetsService from './ngbDataSets.service';

export default class ngbDataSetsController extends baseController {
    static get UID() {
        return 'ngbDataSetsController';
    }

    searchPattern = null;
    nothingFound = null;
    _isLoading = true;
    projectContext;

    service;

    constructor($scope, $timeout, dispatcher, ivhTreeviewBfs, ivhTreeviewMgr, ngbDataSetsService, projectContext) {
        super();
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ivhTreeviewBfs,
            ivhTreeviewMgr,
            projectContext,
            service: ngbDataSetsService
        });
        $scope.$watch('$ctrl.searchPattern', ::this.searchPatternChanged);
        this.initEvents();
        this._isLoading = true;
    }

    async $onInit() {
        await this.refreshDatasets();
    }

    async loadingStarted() {
        this._isLoading = true;
        this.$timeout(::this.$scope.$apply);
    }

    async loadingFinished() {
        this.datasets = await this.service.getDatasets();
        this.nothingFound = false;
        this._isLoading = !this.projectContext.datasetsLoaded;
        this.$timeout(::this.$scope.$apply);
    }

    get isLoading() {
        return this._isLoading;
    }

    events = {
        'datasets:loading:finished': ::this.loadingFinished,
        'datasets:loading:started': ::this.loadingStarted,
        'projectId:change': ::this.onProjectChanged
    };

    async onProjectChanged() {
        await this.service.updateSelectionFromState(this.datasets);
    }

    async select(item, isSelected, tree) {
        const self = this;
        this.$timeout(() => {
            // we should deselect all nested projects - only files should be selected.
            self.service.selectItem(item, isSelected, tree);
            self.$scope.$apply();
        });
    }

    clearSearch() {
        this.searchPattern = null;
    }

    async refreshDatasets() {
        await this.projectContext.refreshDatasets();
    }

    searchPatternChanged() {
        if (this.datasets) {
            this.nothingFound = !ngbDataSetsService.search(this.searchPattern || '', this.datasets);
        } else {
            this.nothingFound = false;
        }
    }
}