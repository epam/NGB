export default class ngbSequencesTableService {

    _currentPage = 1;

    get currentPage () {
        return this._currentPage;
    }
    set currentPage (value) {
        this._currentPage = value ;
    }

    static instance (dispatcher, ngbSequencesPanelService) {
        return new ngbSequencesTableService(dispatcher, ngbSequencesPanelService);
    }

    constructor(dispatcher, ngbSequencesPanelService) {
        Object.assign(this, {dispatcher, ngbSequencesPanelService});
        dispatcher.on('target:identification:changed', this.targetChanged.bind(this));
    }

    get pageSize() {
        return this.ngbSequencesPanelService.pageSize;
    }

    getSequencesResults() {
        const sequenceResults = this.ngbSequencesPanelService.sequencesResults;
        if (!sequenceResults || !sequenceResults.length) return [];
        const start = (this.currentPage - 1) * this.pageSize;
        const end = this.currentPage * this.pageSize;
        const results = sequenceResults.slice(start, end);
        return results;
    }

    targetChanged() {
        this._currentPage = 1;
    }
}
