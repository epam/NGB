export default class ngbProjectInfoEditNoteController {
    isLoadingSave = false;
    isLoadingRemove = false;

    constructor($scope, ngbProjectInfoService) {
        this.ngbProjectInfoService = ngbProjectInfoService;
        this.$scope = $scope;
    }

    static get UID() {
        return 'ngbProjectInfoEditNoteController';
    }

    saveNote() {
        this.isLoadingSave = true;
        this.ngbProjectInfoService.saveNote(this.note).then(data => {
            this.error = data.error ? data.message : '';
            this.isLoadingSave = false;
            this.$scope.$apply();
        });
    }

    deleteNote() {
        this.isLoadingRemove = true;
        this.ngbProjectInfoService.deleteNote(this.note.id).then(data => {
            this.error = data.error ? data.message : '';
            this.isLoadingRemove = false;
            this.$scope.$apply();
        });
    }
}
