export default class ngbStrainLineageNodeController {

    constructor() {
        this.nodeData = JSON.parse(this.nodeDataJson);
        this.toggleTooltip = false;
    }

    static get UID() {
        return 'ngbStrainLineageNodeController';
    }

    close() {
        this.toggleTooltip = false;
    }

}
