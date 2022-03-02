export default class ngbCoveragePanelController {

    isSearchInProgress = false;
    isSearchFailure = false;
    _coverageIndexes = [];
    _coverageIds = {};

    get coverageIndexes() {
        return this._coverageIndexes;
    }

    static get UID() {
        return 'ngbCoveragePanelController';
    }

    constructor(
        $scope,
        dispatcher,
        bamCoverageContext,
        ngbCoveragePanelService) {
        Object.assign(this, {
            $scope,
            dispatcher,
            bamCoverageContext,
            ngbCoveragePanelService
        });
        const updateCoverageIndexes = this.updateCoverageIndexes.bind(this);
        dispatcher.on('bam:coverage:changed', updateCoverageIndexes);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('bam:coverage:changed', updateCoverageIndexes);
        });
    }

    $onDestroy() {
        this.ngbCoveragePanelService.resetInfo();
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        await this.setCoverageIndexes();
        this.setCurrentCoverageIndex();
        this.selectCoverageIndex();
    }

    async updateCoverageIndexes(isCurrentBamClosed) {
        await this.setCoverageIndexes();
        if (isCurrentBamClosed) {
            this.currentCoverageIndex = undefined;
            this.ngbCoveragePanelService.coverageSearchResults = null;
        }
    }

    get errorMessageList() {
        return this.ngbCoveragePanelService.errorMessageList;
    }
    get coverageSearchResults() {
        return this.ngbCoveragePanelService.coverageSearchResults;
    }
    get currentCoverageIndex() {
        return this.ngbCoveragePanelService.currentCoverageIndex;
    }
    set currentCoverageIndex(value) {
        this.ngbCoveragePanelService.currentCoverageIndex = value;
    }
    get currentBamId() {
        return this.bamCoverageContext.currentBamId;
    }
    set currentBamId(value) {
        this.bamCoverageContext.currentBamId = value;
    }

    setCoverageIndexes() {
        const coverageStatistics = this.bamCoverageContext.coverageStatistics;
        const indexes = [];
        for (const bamId in coverageStatistics) {
            if (coverageStatistics.hasOwnProperty(bamId)) {
                for (const item of coverageStatistics[bamId]) {
                    if (item) {
                        const {name, interval, coverageId, bamId} = item;
                        indexes.push({name, interval, coverageId});
                        this._coverageIds[coverageId] = bamId;
                    }                    
                }
            }
        }
        this._coverageIndexes = indexes.sort((a, b) => {
            if (a.name === b.name) {
                return a.interval > b.interval ? 1 : -1;
            } else {
                return a.name > b.name ? 1 : -1;
            }
        });
    }

    async selectCoverageIndex() {
        if (!this.currentCoverageIndex) {
            return;
        }
        this.ngbCoveragePanelService.resetCurrentPages();
        this.ngbCoveragePanelService.sortInfo = null;
        this.ngbCoveragePanelService.displayFilters = false;
        this.ngbCoveragePanelService.clearFilters();
        const coverageId = this.currentCoverageIndex;
        this.currentBamId = this._coverageIds[coverageId];
        this.isSearchInProgress = true;
        const request = await this.ngbCoveragePanelService.setSearchCoverageRequest(coverageId, false);
        await this.ngbCoveragePanelService.searchBamCoverage(request)
            .then(success => {
                this.isSearchFailure = !success;
                this.isSearchInProgress = false;
                this.$scope.$apply();
            });
    }

    setCurrentCoverageIndex() {
        const indexes = this._coverageIndexes;
        if (!indexes.length) {
            return;
        }
        for (let i = 0; i < indexes.length; i++) {
            if (!this.currentCoverageIndex &&
                this._coverageIds[indexes[i].coverageId] === this.currentBamId
            ) {
                this.currentCoverageIndex = indexes[i].coverageId;
            }
        }
    }
}
