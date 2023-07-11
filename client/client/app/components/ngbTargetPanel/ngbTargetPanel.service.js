
const SHORT_DESCRIPTION_LENGTH = 150;

export default class ngbTargetPanelService {

    _identificationData = null;
    _identificationTarget = null;
    _descriptions;
    _shortDescription;

    get shortDescriptionLength() {
        return SHORT_DESCRIPTION_LENGTH;
    }

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
    get shortDescription() {
        return this._shortDescription;
    }

    static instance (appLayout, dispatcher, $sce) {
        return new ngbTargetPanelService(appLayout, dispatcher, $sce);
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
        this.setShortDescription();
        this.dispatcher.emit('description:is:assigned');
    }

    getChipByGeneId(id) {
        const chips = this.allGenes
            .filter(g => g.geneId.toLowerCase() === id.toLowerCase())
            .map(g => g.chip);
        if (chips && chips.length) {
            return chips[0];
        }
    }

    setDescriptions() {
        const titlesByGeneId = (id) => this.getChipByGeneId(id) || '';
        const getDescriptionElements = (description) => {
            const html = this.$sce.trustAsHtml(description);
            const linkRegex = /<a\b[^>]*\bhref=['"](.*?)['"][^>]*>(.*?)<\/a>/gi;
            let match;
            let startIndex = 0;
            const elements = [];

            while ((match = linkRegex.exec(html)) !== null) {
                elements.push({
                    type: 'text',
                    value: match.input.substring(startIndex, match.index)
                });
                elements.push({
                    type: 'link',
                    value: {
                        href: match[1],
                        text: match[2]
                    }
                });
                startIndex = match.index + match[0].length;
            }
            if (match === null && startIndex < description.length) {
                elements.push({
                    type: 'text',
                    value: description.substring(startIndex)
                });
            }
            return elements;
        };
        if (this.identificationData && this.identificationData.description) {
            this._descriptions = Object.entries(this.identificationData.description)
                .map(([geneId, description]) => ({
                    title: titlesByGeneId(geneId),
                    value: getDescriptionElements(description)
                }));
        }
    }

    setShortDescription() {
        const short = [];
        let count = 0;
        const max = this.shortDescriptionLength;
        for (let i = 0; i < this.descriptions.length; i++) {
            const description = this.descriptions[i].value;
            for (let j = 0; j < description.length; j++) {
                const {type, value} = description[j];
                if (count < max) {
                    if (type === 'text') {
                        short.push({
                            type,
                            value: value.substring(0, max - count)
                        });
                        count += value.length;
                    }
                    if (type === 'link') {
                        short.push({
                            type,
                            value
                        });
                        count += value.text.length;
                    }
                } else {
                    this._shortDescription = short;
                    return;
                }
            }
        }
    }
}
