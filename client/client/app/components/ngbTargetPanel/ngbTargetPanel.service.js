export default class ngbTargetPanelService {

    _identificationData = null;
    _identificationTarget = null;
    _descriptions;

    get identificationData() {
        return this._identificationData;
    }
    get identificationTarget() {
        return this._identificationTarget;
    }

    get allGenes() {
        const {interest, translational} = this.identificationTarget;
        return [...interest, ...translational];
    }

    get descriptions() {
        return this._descriptions;
    }

    static instance (appLayout, dispatcher) {
        return new ngbTargetPanelService(appLayout, dispatcher);
    }

    constructor(appLayout, dispatcher) {
        Object.assign(this, {appLayout, dispatcher});
        dispatcher.on('target:launch:finished', this.setIdentificationData.bind(this));
    }

    panelAddTargetPanel() {
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = true;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    panelCloseTargetPanel() {
        this.resetData();
        const layoutChange = this.appLayout.Panels.target;
        layoutChange.displayed = false;
        this.dispatcher.emitSimpleEvent('layout:item:change', {layoutChange});
    }

    resetIdentificationData() {
        this.dispatcher.emit('reset:identification:data');
        this._identificationData = null;
        this._identificationTarget = null;
    }

    setIdentificationData(data, info) {
        this._identificationData = data;
        this._identificationTarget = info;
        this.setDescriptions();
        this.dispatcher.emit('description:is:assigned');
    }

    getChipByGeneId(id) {
        const chips = this.allGenes.filter(g => g.geneId === id).map(g => g.chip);
        if (chips && chips.length) {
            return chips[0];
        }
    }

    setDescriptions() {
        const titlesByGeneId = (id) => this.getChipByGeneId(id) || '';
        if (this.identificationData && this.identificationData.description) {
            this._descriptions = Object.entries(this.identificationData.description)
                .map(([geneId, description]) => ({
                    title: titlesByGeneId(geneId),
                    value: description
                }));
        }
    }
}
