import {mapTrackFn} from '../../ngbDataSets/internal/utilities';

const LEFT_CLICK = 1;

export default class ngbStrainLineageNodeController {

    isDrag = false;
    navigationInProcess = false;

    constructor(projectContext) {
        this.projectContext = projectContext;
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
        const payload = this.getOpenReferencePayload(referenceObj);
        if (payload) {
            this.projectContext.changeState(payload);
        }
    }

    getOpenReferencePayload(referenceObj) {
        if (referenceObj && this.projectContext.datasets) {
            // we'll open first dataset of this reference
            const tree = this.projectContext.datasets || [];
            const find = (items = []) => {
                const projects = items.filter(item => item.isProject);
                const [dataset] = projects.filter(item => item.reference && item.reference.id === referenceObj.id);
                if (dataset) {
                    return dataset;
                }
                for (const project of projects) {
                    const nested = find(project.nestedProjects);
                    if (nested) {
                        return nested;
                    }
                }
                return null;
            };
            const dataset = find(tree);
            if (dataset) {
                const tracks = [dataset.reference];
                const tracksState = [mapTrackFn(dataset.reference)];
                return {
                    tracks,
                    tracksState,
                    reference: dataset.reference,
                    shouldAddAnnotationTracks: true
                };
            }
        }
        return null;
    }
}
