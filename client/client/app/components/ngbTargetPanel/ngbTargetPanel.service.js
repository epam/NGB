import processLinks from './utilities/process-links';

const FORMAT = {
    XLSX: 'xlsx',
    HTML: 'html'
}

const STATUS_OPTIONS = {
    SAVE: 'SAVE',
    SAVED: 'SAVED'
};

export default class NgbTargetPanelService {

    get format() {
        return FORMAT;
    }

    get statusOptions() {
        return STATUS_OPTIONS;
    }

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

    get allChips() {
        return this.allGenes.map(g => g.chip);
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

    get saveStatus() {
        return this.isIdentificationSaved
            ? this.statusOptions.SAVED
            : (this._saveStatus ? this._saveStatus : this.statusOptions.SAVE);
    }
    set saveStatus(value) {
        this._saveStatus = value;
    }

    get isIdentificationSaved() {
        if (!this.identificationTarget || !this.identificationTarget.target) return false;
        const {identifications} = this.identificationTarget.target;
        if (!identifications || !identifications.length) return false;
        const isSaved = identifications.some(item => {
            const isEqual = (current, saved) => {
                if (current.length !== saved.length) return false;
                const sortedCurrent = [...current].sort();
                const sortedSaved = [...saved].sort();
                return sortedSaved.every((item, index) => item === sortedCurrent[index]);
            }
            return isEqual(this.geneIdsOfInterest, item.genesOfInterest)
                && isEqual(this.translationalGeneIds, item.translationalGenes);
        })
        return isSaved;
    }

    static instance (appLayout, dispatcher, $sce, targetDataService) {
        return new NgbTargetPanelService(appLayout, dispatcher, $sce, targetDataService);
    }

    constructor(appLayout, dispatcher, $sce, targetDataService) {
        Object.assign(this, {appLayout, dispatcher, $sce, targetDataService});
        dispatcher.on('target:launch:finished', this.setIdentificationData.bind(this));
        dispatcher.on('target:identification:status:update', this.resetSaveStatus.bind(this));
    }

    resetSaveStatus() {
        this._saveStatus = undefined;
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
        this.resetSaveStatus();
        this._descriptions = null;
        this.dispatcher.emit('target:identification:changed', this.identificationTarget);
    }

    setIdentificationData(data, info) {
        this._identificationData = data;
        this._identificationTarget = this.setChips(info);
        this.setDescriptions();
        this.dispatcher.emit('target:identification:changed', this.identificationTarget);
    }

    setChips(info) {
        const {interest = [], translational = []} = info;
        const setChip = (genes) => {
            for (let i = 0; i < genes.length; i++) {
                const gene = genes[i];
                gene.chip = gene.chip || `${gene.geneName}${gene.speciesName ? ` (${gene.speciesName})` : ''}`;
            }
        };
        setChip(interest);
        setChip(translational);
        return info;
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

    exportResults(format) {
        const {target} = this.identificationTarget || {};
        if (target && target.id) {
            if (format === this.format.XLSX) {
                return this.exportExcel(target.id);
            }
            if (format === this.format.HTML) {
                return this.exportHtml(target.id);
            }
        } else {
            if (format === this.format.XLSX) {
                return this.exportExcelGeneId();
            }
            if (format === this.format.HTML) {
                return this.exportHtmlGeneId();
            }
        }
    }

    exportExcel(targetId) {
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => resolve(true));
        }
        return this.targetDataService.getTargetExcelReport(this.geneIdsOfInterest, this.translationalGeneIds, targetId);
    }

    exportHtml(targetId) {
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length || !targetId) {
            return new Promise(resolve => resolve(true));
        }
        return this.targetDataService.getTargetHtmlReport(this.geneIdsOfInterest, this.translationalGeneIds, targetId);
    }

    exportExcelGeneId() {
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => resolve(true));
        }
        return this.targetDataService.getTargetExcelReportGeneId(this.geneIdsOfInterest[0]);
    }

    exportHtmlGeneId() {
        if (!this.geneIdsOfInterest || !this.geneIdsOfInterest.length) {
            return new Promise(resolve => resolve(true));
        }
        return this.targetDataService.getTargetHtmlReportGeneId(this.geneIdsOfInterest[0]);
    }

    saveIdentification(name) {
        const {target} = this.identificationTarget || {};
        const genesOfInterest = this.geneIdsOfInterest;
        const translationalGenes = this.translationalGeneIds;
        if (!target || !name || !genesOfInterest.length || !translationalGenes.length) {
            return new Promise(resolve => {
                resolve(false);
            });
        }
        const request = {
            name,
            targetId: target.id,
            genesOfInterest,
            translationalGenes,
        };
        return this.targetDataService.postIdentification(request);
    }
}
