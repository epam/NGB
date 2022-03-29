export default class ngbPathwaysAnnotationAddDlgController {
    constructor(ngbPathwaysAnnotationService, $mdDialog, annotation, pathwayId) {
        this.ngbPathwaysAnnotationService = ngbPathwaysAnnotationService;
        this.$mdDialog = $mdDialog;
        if (annotation) {
            this.annotation = {
                ...annotation
            };
            if (this.annotation.type === this.ngbPathwaysAnnotationService.annotationTypeList.HEATMAP
                || this.annotation.type === this.ngbPathwaysAnnotationService.annotationTypeList.CSV) {
                this.colorScheme = this.annotation.colorScheme.copy();
            }
        } else {
            this.annotation = {
                name: undefined,
                type: undefined,
                config: null,
                pathwayId: pathwayId
            };
            this.colorScheme = null;
        }
    }

    static get UID() {
        return 'ngbPathwaysAnnotationAddDlgController';
    }

    get isStateValid() {
        return this.annotation.type !== undefined
            && (this.annotation.type === this.ngbPathwaysAnnotationService.annotationTypeList.MANUAL
                || this.annotation.header !== undefined);
    }


    save() {
        this.annotation = {
            ...this.annotation,
            colorScheme: this.colorScheme
        };
        this.ngbPathwaysAnnotationService.save(this.annotation);
        this.close();
    }

    close() {
        this.$mdDialog.hide();
    }
}
