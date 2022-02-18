export default class BamCoverageContext {

    _isCoverageStatistics = {};
    _coverageStatistics = {};
    _currentBamId = null;

    get coverageStatistics () {
        return this._coverageStatistics;
    }

    get isCoverageStatistics () {
        return this._isCoverageStatistics;
    }

    get currentBamId() {
        return this._currentBamId;
    }
    set currentBamId(value) {
        this._currentBamId = value;
    }

    static instance(dispatcher, projectContext, bamDataService) {
        return new BamCoverageContext(dispatcher, projectContext, bamDataService);
    }

    constructor (dispatcher, projectContext, bamDataService) {
        Object.assign(this, {dispatcher, projectContext, bamDataService});
        this.dispatcher.on('tracks:state:change', this.toggleTrack.bind(this));
        this.dispatcher.on('reference:change', this.resetCoverageStatistics.bind(this));
    }

    toggleTrack() {
        const bamTracksIds = this.projectContext.getActiveTracks()
            .filter(track => track.format === 'BAM')
            .map(track => track.id);
        for (const bamId in this._coverageStatistics) {
            if (!bamTracksIds.includes(Number(bamId))) {
                this.deleteBamCoverage(bamId);
            }
        }
    }

    async setBamCoverage({bamId, bamName}) {
        await this.bamDataService.getBamCoverage(bamId)
            .then((result) => {
                if (result.length) {
                    this._isCoverageStatistics[bamId] = true;
                    this._coverageStatistics[bamId] = result.map(coverage => ({
                        name: bamName,
                        bamId: coverage.bamId,
                        interval: coverage.step,
                        coverageId: coverage.coverageId,
                        coverage: coverage.coverage
                    }));
                    this.dispatcher.emitSimpleEvent('bam:coverage:changed', false);
                } else {
                    this._isCoverageStatistics[bamId] = false;
                    this._coverageStatistics[bamId] = [];
                }
            });
    }

    deleteBamCoverage(bamId) {
        const wereCoverageStatistics = this._isCoverageStatistics[bamId];
        delete this._isCoverageStatistics[bamId];
        delete this._coverageStatistics[bamId];
        if (JSON.stringify(this._isCoverageStatistics) === '{}' ||
            Object.values(this._isCoverageStatistics).filter(item => item).length === 0
        ) {
            this.dispatcher.emitSimpleEvent('bam:coverage:empty');
            return;
        }
        if (wereCoverageStatistics) {
            const isCurrentBamClosed = this._currentBamId === Number(bamId);
            if (isCurrentBamClosed) {
                this._currentBamId = null;
            }
            this.dispatcher.emitSimpleEvent('bam:coverage:changed', isCurrentBamClosed);
        }
    }

    resetCoverageStatistics() {
        this._isCoverageStatistics = {};
        this._coverageStatistics = {};
        this._currentBamId = null;
    }
}
