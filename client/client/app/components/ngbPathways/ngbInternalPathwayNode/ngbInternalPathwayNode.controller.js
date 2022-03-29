const LEFT_CLICK = 1;
const MOUSE_DRAG_THRESHOLD_PX = 2;

const hdrTypeList = {
    GENE: 'Gene'
};

export default class ngbInternalPathwayNodeController {

    isDrag = false;
    navigationInProcess = false;
    highlightStyle;

    constructor(projectContext, $sce, ngbInternalPathwaysResultService) {
        this.projectContext = projectContext;
        this.ngbInternalPathwaysResultService = ngbInternalPathwaysResultService;
        this.nodeData = JSON.parse(this.nodeDataJson);
        this.highlightNode();
        if (this.nodeData.label) {
            this.label = $sce.trustAsHtml(this.nodeData.label
                .replace(new RegExp('-', 'g'), '\u2011')
                .replace(new RegExp(' ', 'g'), '\u00A0'));
        }
        if (this.nodeData.unitsOfInformation && this.nodeData.unitsOfInformation.length) {
            this.unitOfInformation = this.nodeData.unitsOfInformation[0].label.text;
        }
        if (this.nodeData.ttipData) {
            this.nodeData.ttipData.forEach(data => {
                if (data.hdr === hdrTypeList.GENE) {
                    this.geneId = data.data[0].text;
                    this.geneName = $sce.trustAsHtml(this.geneId);
                }
            });
        }
    }

    static get UID() {
        return 'ngbInternalPathwayNodeController';
    }

    get selected() {
        return this.nodeData &&
            this.ngbInternalPathwaysResultService.selectedElementId === this.nodeData.id;
    }

    onMouseDown(event) {
        this.isDrag = false;
        this.mouseDownPosition = {
            x: event.screenX,
            y: event.screenY
        };
    }

    onMouseMove(event) {
        if (this.mouseDownPosition) {
            const delta = Math.sqrt(
                (this.mouseDownPosition.x - event.screenX) ** 2 +
                (this.mouseDownPosition.y - event.screenY) ** 2
            );
            this.isDrag = this.isDrag || (delta > MOUSE_DRAG_THRESHOLD_PX);
        } else {
            this.isDrag = true;
        }
    }

    onMouseUp(event) {
        this.mouseDownPosition = undefined;
        if (this.navigationInProcess) {
            this.navigationInProcess = false;
        } else if (!this.isDrag && event.which === LEFT_CLICK) {
            this.onElementClick({
                data: {
                    ...this.nodeData,
                    title: this.nodeData.fullTitle
                }
            });
        }
    }

    navigate() {
        if (this.nodeData.taxId && this.geneId) {
            this.navigateToGene();
        }
    }

    async navigateToGene() {
        if (this.navigationInProcess) {
            return;
        }
        this.navigationInProcess = true;
        const [referenceObj] = (this.projectContext.references || [])
            .filter(reference => reference.species && Number(reference.species.taxId) === Number(this.nodeData.taxId));
        if (!referenceObj) {
            return;
        }
        const gene = await this.ngbInternalPathwaysResultService.searchGenes(referenceObj.id, this.geneId);
        if (!gene) {
            return;
        }
        const chromosomeObj = gene.chromosome,
            endIndex = gene.endIndex,
            startIndex = gene.startIndex;
        if (chromosomeObj && chromosomeObj.id && startIndex && endIndex) {
            const range = Math.abs(endIndex - startIndex);
            const start = Math.min(startIndex, endIndex) - range / 10.0;
            const end = Math.max(startIndex, endIndex) + range / 10.0;
            const referenceChanged = !this.projectContext.reference ||
                this.projectContext.reference.id !== referenceObj.id;
            const payload = referenceChanged
                ? this.ngbInternalPathwaysResultService.getOpenReferencePayload(this.projectContext, referenceObj)
                : {};
            payload.chromosome = chromosomeObj;
            payload.viewport = {
                start,
                end
            };
            this.projectContext.changeState(payload);
        }
        this.navigationInProcess = false;
    }

    highlightNode() {
        this.highlightStyle = this.nodeData.highlightColor
            ? {'background-color': this.nodeData.highlightColor}
            : undefined;
    }
}
