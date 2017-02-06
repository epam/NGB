import baseController from '../../shared/baseController';
import ngbDataSetsService from './ngbDataSets.service';

export default class ngbDataSetsController extends baseController {
    static get UID() {
        return 'ngbDataSetsController';
    }

    searchPattern = null;
    nothingFound = null;
    noDatasets = false;
    _isLoading = true;
    projectContext;

    service;

    constructor($mdDialog, $scope, $timeout, dispatcher, ivhTreeviewBfs, ivhTreeviewMgr, ngbDataSetsService, projectContext) {
        super();
        Object.assign(this, {
            $mdDialog,
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

        const self = this;
        this.tracksStateChangeListener = async () => {
            await self.service.updateSelectionFromState(self.datasets);
        };
        const tracksStateChangeListener = ::this.tracksStateChangeListener;
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('tracks:state:change', tracksStateChangeListener);
        });
        dispatcher.on('tracks:state:change', tracksStateChangeListener);

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
        this.noDatasets = this.datasets.length === 0;
        this._isLoading = !this.projectContext.datasetsLoaded;
        this.$timeout(::this.$scope.$apply);
    }

    get isLoading() {
        return this._isLoading;
    }

    events = {
        'datasets:loading:finished': ::this.loadingFinished,
        'datasets:loading:started': ::this.loadingStarted,
        'reference:change': ::this.onProjectChanged
    };

    async onProjectChanged() {
        await this.service.updateSelectionFromState(this.datasets);
    }

    async select(item, isSelected, tree) {
        const self = this;
        if (!self.service.checkSelectionAvailable(item, isSelected)) {
            const reference = self.service.getItemReference(item);
            this.$timeout(() => {
                self.service.deselectItem(item, tree);
                self.$scope.$apply();
            });
            const confirm = self.$mdDialog.confirm()
                .title(`Switch reference ${self.projectContext.reference ? self.projectContext.reference.name : ''}${reference ? ` to ${reference.name}` : ''}?`)
                .textContent('All open tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');
            self.$mdDialog.show(confirm).then(function() {
                self.service.selectItem(item, isSelected, tree);
            }, function() {});
        } else {
            this.$timeout(() => {
                self.service.selectItem(item, isSelected, tree);
                self.$scope.$apply();
            });
        }
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