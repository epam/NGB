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

    constructor($mdDialog, $scope, $element, $timeout, dispatcher, ngbDataSetsService, projectContext) {
        super();
        Object.assign(this, {
            $mdDialog,
            $scope,
            $timeout,
            $element,
            dispatcher,
            projectContext,
            service: ngbDataSetsService
        });
        $scope.$watch('$ctrl.searchPattern', ::this.searchPatternChanged);
        this.initEvents();
        this._isLoading = true;

        const self = this;
        this.tracksStateChangeListener = async() => {
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
        if (!this.projectContext.datasetsAreLoading) {
            this.datasets = await this.service.getDatasets();
            this.nothingFound = false;
        }
        this.noDatasets = !this.datasets || this.datasets.length === 0;
        this._isLoading = !this.projectContext.datasetsLoaded;
        this.$timeout(::this.$scope.$apply);
        this.onResize();
    }

    get isLoading() {
        return this._isLoading;
    }

    events = {
        'datasets:loading:finished': ::this.loadingFinished,
        'datasets:loading:started': ::this.loadingStarted,
        'datasets:filter:changed': ::this.loadingFinished,
        'reference:change': ::this.onProjectChanged,
        'activeDataSets': ::this.onResize
    };

    async onProjectChanged() {
        await this.service.updateSelectionFromState(this.datasets);
        this.onResize();
    }

    async select(item, isSelected, tree) {
        const self = this;
        if (!self.service.checkSelectionAvailable(item, isSelected)) {
            const reference = self.service.getItemReference(item);
            this.$timeout(() => {
                self.service.deselectItem(item);
                self.$scope.$apply();
            });
            const confirm = self.$mdDialog.confirm()
                .title(`Switch reference ${self.projectContext.reference ? self.projectContext.reference.name : ''}${reference ? ` to ${reference.name}` : ''}?`)
                .textContent('All opened tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');
            self.$mdDialog.show(confirm).then(function () {
                self.service.selectItem(item, isSelected, tree);
            }, function () {
            });
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

    onResize() {
        this.$timeout(() => {
            this.$element.resize();
        });
    }

    toggleSelected(node) {
        node.__selected = !node.__selected;
        this.toggle(node);
    }

    toggle(node) {
        if (node.__selected) {
            node.__expanded = true;
        }
        if (node.isProject && !node.__selected) {
            this.service.deselectItem(node);
        }

        this.select(node, node.__selected, this.datasets);
    }

    expanded(node) {
        node.__expanded = !node.__expanded;
        this.service.toggle(node);
    }

    filter() {
        return this.service.filter;
    }

    getTemplateNode(node) {
        if (node.isProject)
            return 'ngbDataSetsParentNode.tpl.html';
        else
            return 'ngbDataSetsTerminalNode.tpl.html';
    }

    getTrackFileName(track) {
        if (!track.isLocal) {
            return track.prettyName || track.name;
        } else {
            const fileName = track.name;
            if (!fileName || !fileName.length) {
                return null;
            }
            let list = fileName.split('/');
            list = list[list.length - 1].split('\\');
            return list[list.length - 1];
        }
    }
}
