export default class ngbGenomicsPanelController {

    targetModel = {};
    queryModel = {};
    geneOptions = [];
    proteinOptions = {
        target: [],
        query: []
    };

    static get UID() {
        return 'ngbGenomicsPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbGenomicsPanelService, ngbTargetPanelService, ngbSequencesPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbGenomicsPanelService, ngbTargetPanelService, ngbSequencesPanelService});
    }

    get loadingData() {
        return this.ngbGenomicsPanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbGenomicsPanelService.loadingData = value;
    }
    get failedResult() {
        return this.ngbGenomicsPanelService.failedResult;
    }
    get errorMessageList() {
        return this.ngbGenomicsPanelService.errorMessageList;
    }

    get genesIds() {
        return this.ngbTargetPanelService.genesIds;
    }

    get allSequences () {
        return this.ngbSequencesPanelService.allSequences;
    }

    get targetId() {
        const {target} = this.ngbTargetPanelService.identificationTarget || {};
        return target.id;
    }

    getChipByGeneId (id) {
        return this.ngbTargetPanelService.getChipByGeneId(id);
    }

    $onInit() {
        this.initialize();
    }

    async initialize() {
        if (this.genesIds) {
            this.geneOptions = this.genesIds.map(id => ({
                geneId: id,
                chip: this.getChipByGeneId(id)
            }));
        }
    }

    get targetGeneOptions() {
        return this.geneOptions.filter(o => this.queryModel.gene !== o.geneId);
    }

    get queryGeneOptions() {
        return this.geneOptions.filter(o => this.targetModel.gene !== o.geneId);
    }

    onChangeGene(name, geneId) {
        if (!geneId) {
            this.proteinOptions[name] = [];
        } else {
            const data = this.allSequences[geneId.toLowerCase()] || {};
            const proteins = (data.sequences || [])
                .map(s => (s.protein || {}).id)
                .filter(p => p);
            this.proteinOptions[name] = proteins;
        }
    }

    get targetProteinOptions() {
        return this.proteinOptions.target.filter(o => o !== this.queryModel.protein);
    }

    get queryProteinOptions() {
        return this.proteinOptions.query.filter(o => o !== this.targetModel.protein);
    }

    async alignComparison() {
        this.loadingData = true;
        const targetProtein = this.targetModel.protein;
        const queryProtein = this.queryModel.protein;
        if (!this.targetId || !targetProtein || !queryProtein) {
            this._loadingData = false;
            return;
        }
        const sequenceIds = {
            firstSequenceId: targetProtein,
            secondSequenceId: queryProtein
        };
        await this.ngbGenomicsPanelService.getTargetAlignment(this.targetId, sequenceIds);
        this.dispatcher.emit('target:identification:alignment:updated', true);
        this.$timeout(() => this.$scope.$apply());
    }

    viewOnTrack() {}
}
