const LEFT_CLICK = 1;

const MOUSE_DRAG_THRESHOLD_PX = 2;

export default class ngbInternalPathwayNodeController {

    isDrag = false;
    navigationInProcess = false;
    highlightStyle;

    constructor(projectContext, $sce, ngbInternalPathwaysResultService) {
        this.projectContext = projectContext;
        this.ngbInternalPathwaysResultService = ngbInternalPathwaysResultService;
        this.nodeData = JSON.parse(this.nodeDataJson);
        this.highlightNode();
        this.nodeData.label = $sce.trustAsHtml(this.nodeData.label);
        if (this.nodeData.unitsOfInformation && this.nodeData.unitsOfInformation.length) {
            this.unitOfInformation = this.nodeData.unitsOfInformation[0].label.text;
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

    navigate(event, element) {
        if (element.referenceId) {
            this.navigateToReference(event, element.referenceId);
        } else if (element.projectId) {
            this.navigateToDataset(event, element.projectId);
        }
    }

    navigateToReference(event, referenceId) {
        this.navigationInProcess = true;
        if (!referenceId || !this.projectContext || !this.projectContext.references || !this.projectContext.references.length) {
            return;
        }
        // const referenceObj = this.projectContext.references.filter(reference => reference.id === referenceId).pop();
        // const payload = this.ngbInternalPathwayService.getOpenReferencePayload(this.projectContext, referenceObj);
        // if (payload) {
        //     this.projectContext.changeState(payload);
        // }
    }

    navigateToDataset(event, projectId) {
        this.navigationInProcess = true;
        if (!projectId || !this.projectContext || !this.projectContext.datasets || !this.projectContext.datasets.length) {
            return;
        }
        // const payload = this.ngbInternalPathwayService.getOpenDatasetPayload(this.projectContext.datasets, projectId);
        // if (payload) {
        //     this.projectContext.changeState(payload);
        // }
    }

    highlightNode() {
        this.highlightStyle = this.nodeData.highlightColor
            ? {'background-color': this.nodeData.highlightColor}
            : undefined;
    }
}
