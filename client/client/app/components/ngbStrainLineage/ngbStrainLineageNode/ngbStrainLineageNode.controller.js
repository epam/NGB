const LEFT_CLICK = 1;

export default class ngbStrainLineageNodeController {

    isDrag = false;
    navigationInProcess = false;

    constructor(projectContext, ngbStrainLineageService) {
        this.projectContext = projectContext;
        this.ngbStrainLineageService = ngbStrainLineageService;
        this.nodeData = JSON.parse(this.nodeDataJson);
    }

    static get UID() {
        return 'ngbStrainLineageNodeController';
    }

    onMouseMove() {
        this.isDrag = true;
    }

    onMouseUp(event) {
        if (this.navigationInProcess) {
            this.navigationInProcess = false;
        } else if (!this.isDrag && event.which === LEFT_CLICK) {
            this.onElementClick({data: this.nodeData});
        }
    }

    onMouseDown() {
        this.isDrag = false;
    }

    navigateToReference(event, referenceId) {
        this.navigationInProcess = true;
        if (!referenceId || !this.projectContext || !this.projectContext.references || !this.projectContext.references.length) {
            return;
        }
        const referenceObj = this.projectContext.references.filter(reference => reference.id === referenceId).pop();
        const payload = this.ngbStrainLineageService.getOpenReferencePayload(this.projectContext, referenceObj);
        if (payload) {
            this.projectContext.changeState(payload);
        }
    }

}
