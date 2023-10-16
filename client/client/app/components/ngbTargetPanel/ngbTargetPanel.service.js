import processLinks from './utilities/process-links';

export default class NgbTargetPanelService {

    _identificationData = null;
    _identificationTarget = null;
    _descriptions;

    get identificationData() {
        return this._identificationData;
    }
    get identificationTarget() {
        return this._identificationTarget;
    }

    get genesIds() {
        const {
            interest = [],
            translational = []
        } = this.identificationTarget || {};
        return [...new Set([...interest.map((o) => o.geneId), ...translational.map((o) => o.geneId)])];
    }

    get allGenes() {
        const {
            interest = [],
            translational = []
        } = this.identificationTarget || {};
        const allGenes = [...interest, ...translational];
        return this.genesIds.map((id) => allGenes.find((o) => o.geneId === id)).filter(Boolean);
    }

    get geneIdsOfInterest() {
        return ((this.identificationTarget || {}).interest || [])
            .map(i => i.geneId);
    }

    get translationalGeneIds() {
        return ((this.identificationTarget || {}).translational || [])
            .map(i => i.geneId);
    }

    get descriptions() {
        return this._descriptions;
    }

    static instance (appLayout, dispatcher, $sce) {
        return new NgbTargetPanelService(appLayout, dispatcher, $sce);
    }

    constructor(appLayout, dispatcher, $sce) {
        Object.assign(this, {appLayout, dispatcher, $sce});
        dispatcher.on('target:launch:finished', this.setIdentificationData.bind(this));
    }

    panelAddTargetPanel() {
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    resetIdentificationData() {
        this.dispatcher.emit('target:identification:reset');
        this._identificationData = null;
        this._identificationTarget = null;
        this._descriptions = null;
        this.dispatcher.emit('target:identification:changed', this.identificationTarget);
    }

    setIdentificationData(data, info) {
        this._identificationData = data;
        this._identificationTarget = info;
        this.setDescriptions();
        this.dispatcher.emit('target:identification:changed', this.identificationTarget);
    }

    getChipByGeneId(id) {
        const chips = this.allGenes
            .filter(g => g.geneId.toLowerCase() === id.toLowerCase())
            .map(g => g.chip);
        if (chips && chips.length) {
            return chips[0];
        }
    }

    getGeneIdByChip(chip) {
        const geneIds = this.allGenes
            .filter(g => g.chip === chip)
            .map(g => g.geneId)
        if (geneIds && geneIds.length) {
            return geneIds[0].toLowerCase();
        }
    }

    setDescriptions() {
        const titlesByGeneId = (id) => this.getChipByGeneId(id) || '';
        if (this.identificationData && this.identificationData.description) {
            this._descriptions = Object.entries(this.identificationData.description)
                .map(([geneId, description]) => ({
                    title: titlesByGeneId(geneId),
                    html: this.$sce.trustAsHtml(processLinks(description))
                }));
        }
    }
}
