export default class ngbProjectInfoEditNoteController {
    isLoadingSave = false;
    isLoadingRemove = false;

    constructor($scope, ngbProjectInfoService) {
        this.$scope = $scope;
        this.ngbProjectInfoService = ngbProjectInfoService;
    }

    static get UID() {
        return 'ngbProjectInfoEditNoteController';
    }

    handleClickSaveNote() {
        if (this.isLoadingSave || this.isLoadingRemove) {
            return;
        }
        this.isLoadingSave = true;
        this.ngbProjectInfoService.saveNote(this.note)
            .then(data => {
                this.error = data.error ? data.message : '';
                this.isLoadingSave = false;
                this.$scope.$apply();
            });
    }

    handleClickCancelNote() {
        this.ngbProjectInfoService.cancelNote();
    }

    handleClickDeleteNote() {
        if (this.isLoadingSave || this.isLoadingRemove) {
            return;
        }
        this.isLoadingRemove = true;
        this.ngbProjectInfoService.deleteNote(this.note.id)
            .then(data => {
                this.error = data.error ? data.message : '';
                this.isLoadingRemove = false;
                this.$scope.$apply();
            });
    }
}
