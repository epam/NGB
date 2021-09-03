export default class ngbProjectInfoEditNoteController {
    isLoading = false;

    constructor($scope, dispatcher, ngbProjectInfoService) {
        this.ngbProjectInfoService = ngbProjectInfoService;
    }

    static get UID() {
        return 'ngbProjectInfoEditNoteController';
    }

    saveNote() {
        this.isLoading = true;
        this.ngbProjectInfoService.saveNote(this.note).then(data => {
            if (data.error) {
                this.error = 'Error during adding note happened';
            }
            this.isLoading = false;
        });
    }
}
