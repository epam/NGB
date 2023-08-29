const MODEL = {
    TARGET: 'target',
    QUERY: 'query'
};

export default class ngbGenomicsPanelController {

    targetModel = {};
    queryModel = {};
    geneOptions = [];
    proteinOptions = {
        target: [],
        query: []
    };
    allProteins = [];

    static get UID() {
        return 'ngbGenomicsPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbGenomicsPanelService, ngbTargetPanelService, ngbSequencesPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbGenomicsPanelService, ngbTargetPanelService, ngbSequencesPanelService});
        dispatcher.on('target:identification:sequences:results:updated', this.setAllProteinOptions.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:sequences:results:updated', this.setAllProteinOptions.bind(this));
        });
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
                geneId: id.toLowerCase(),
                chip: this.getChipByGeneId(id)
            }));
            this.setAllProteinOptions();
        }
    }

    get targetGeneOptions() {
        return this.geneOptions.filter(o => this.queryModel.gene !== o.geneId);
    }

    get queryGeneOptions() {
        return this.geneOptions.filter(o => this.targetModel.gene !== o.geneId);
    }

    isProteinOptionDisabled(geneId) {
        return !this.allProteins[geneId] || !this.allProteins[geneId].length;
    }

    getProteins(geneId) {
        if (!this.allSequences) return;
        const data = this.allSequences[geneId.toLowerCase()] || {};
        const proteins = (data.sequences || [])
            .map(s => (s.protein || {}).id)
            .filter(p => p);
        return proteins;
    }

    setAllProteinOptions() {
        this.allProteins = this.geneOptions.reduce((acc, gene) => {
            const {geneId} = gene;
            acc[geneId.toLowerCase()] = this.getProteins(geneId);
            return acc;
        }, {});
        this.$timeout(() => this.$scope.$apply());
    }

    onChangeGene(name, geneId) {
        if (name === MODEL.TARGET) {
            this.targetModel.protein = undefined;
        }
        if (name === MODEL.QUERY) {
            this.queryModel.protein = undefined;
        }
        if (!geneId) {
            this.proteinOptions[name] = [];
        } else {
            this.proteinOptions[name] = this.allProteins[geneId.toLowerCase()];
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
