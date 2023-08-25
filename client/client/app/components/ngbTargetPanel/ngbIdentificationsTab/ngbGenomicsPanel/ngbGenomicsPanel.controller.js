export default class ngbGenomicsPanelController {

    _loadingData = false;
    _failedResult = false;
    _errorMessageList = null;
    targetModel = {};
    queryModel = {};
    showAlignButton = true;
    showTrackButton = true;

    get loadingData() {
        return this._loadingData;
    }
    get failedResult() {
        return this._failedResult;
    }
    get errorMessageList() {
        return this._errorMessageList;
    }

    static get UID() {
        return 'ngbGenomicsPanelController';
    }

    constructor(ngbTargetPanelService) {
        Object.assign(this, {ngbTargetPanelService});
        this.alignments = [{
            queryStart: 0,
            sequenceStart: 0,
            queryEnd: 10,
            sequenceEnd: 10,
            querySequence: 'AAA',
            diff: 'diff',
            sequence: 'TTT'
        }];
    }

    get genesIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    getChipByGeneId (id) {
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        if (this.genesIds) {
            this.options = this.genesIds.map(id => ({
                geneId: id,
                chip: this.getChipByGeneId(id)
            }));
        }
    }

    get targetOptions() {
        const arr = this.options.filter(o => this.queryModel.gene !== o.geneId);
        return arr;
    }

    get queryOptions() {
        const arr = this.options.filter(o => this.targetModel.gene !== o.geneId);
        return arr;
    }

    alignComparison() {}

    viewOnTrack() {}
}
