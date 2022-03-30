export default class ngbPathwaysAnnotationAddDlgController {
    constructor(ngbPathwaysAnnotationService, $mdDialog, annotation, pathwayId) {
        this.ngbPathwaysAnnotationService = ngbPathwaysAnnotationService;
        this.$mdDialog = $mdDialog;
        if (annotation) {
            this.annotation = {
                ...annotation
            };
            const list = this.ngbPathwaysAnnotationService.annotationTypeList;
            if ([list.HEATMAP, list.CSV, list.VCF].includes(this.annotation.type)) {
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
        return this.annotation.type !== undefined && this.annotation.type !== null
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
