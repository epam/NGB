export default class NgbInternalPathwayNodeService {
    static instance() {
        return new NgbInternalPathwayNodeService();
    }

    constructor() {
        this.annotations = new Map();
    }

    clearAnnotations() {
        this.annotations.clear();
    }

    clearAnnotation(node) {
        if (node && this.annotations.has(node)) {
            this.annotations.delete(node);
        }
    }

    annotateNode(node, annotations) {
        if (!node) {
            return;
        }
        if (!annotations || !annotations.length) {
            this.clearAnnotation(node);
        } else {
            this.annotations.set(node, annotations);
        }
    }

    getAnnotationsForNode(node) {
        if (this.annotations.has(node)) {
            return this.annotations.get(node);
        }
        return undefined;
    }
}
