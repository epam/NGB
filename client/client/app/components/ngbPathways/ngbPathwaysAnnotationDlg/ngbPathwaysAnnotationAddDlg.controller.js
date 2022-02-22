export default class ngbPathwaysAnnotationAddDlgController {
    constructor(ngbPathwaysAnnotationService, $mdDialog, annotation, pathwayId) {
        this.ngbPathwaysAnnotationService = ngbPathwaysAnnotationService;
        this.$mdDialog = $mdDialog;
        if (annotation) {
            this.annotation = annotation;
        } else {
            this.annotation = {
                name: undefined,
                type: undefined,
                config: null,
                pathwayId: pathwayId
            };
        }
    }

    static get UID() {
        return 'ngbGenesTableDownloadDlgController';
    }

    get isStateValid() {
        return this.annotation.type !== undefined && this.annotation.header !== undefined;
    }


    save() {
        this.ngbPathwaysAnnotationService.save(this.annotation);
        this.close();
    }

    close() {
        this.$mdDialog.hide();
    }
}
