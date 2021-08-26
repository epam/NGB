export default class ngbBookmarksFilterInputController {
    prevValue;
    value;

    constructor(ngbBookmarksTableService) {
        this.ngbBookmarksTableService = ngbBookmarksTableService;
        this.prevValue = this.value = this.ngbBookmarksTableService.bookmarksFilter[this.field.field];
    }

    static get UID() {
        return 'ngbBookmarksFilterInputController';
    }

    apply() {
        if (!this.ngbBookmarksTableService.canScheduleFilterBookmarks()) {
            return;
        }
        if (this.prevValue !== this.value) {
            this.prevValue = this.value;
            this.ngbBookmarksTableService.bookmarksFilter[this.field.field] =
                (this.value && this.value.length) ? this.value : undefined;
            this.ngbBookmarksTableService.scheduleFilterBookmarks();
        }
    }
}
