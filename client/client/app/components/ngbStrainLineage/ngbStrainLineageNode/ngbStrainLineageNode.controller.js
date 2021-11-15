const LEFT_CLICK = 1;

export default class ngbStrainLineageNodeController {

    constructor() {
        this.nodeData = JSON.parse(this.nodeDataJson);
        this.isDrag = false;
    }

    static get UID() {
        return 'ngbStrainLineageNodeController';
    }

    onMouseMove() {
        this.isDrag = true;
    }

    onMouseUp(event) {
        if (!this.isDrag && event.which === LEFT_CLICK) {
            this.onElementClick({data: this.nodeData});
        }
    }

    onMouseDown() {
        this.isDrag = false;
    }

    navigateToTrack(event) {
        event.stopPropagation();
    }

}
