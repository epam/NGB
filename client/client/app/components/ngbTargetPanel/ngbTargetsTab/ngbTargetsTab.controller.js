export default class ngbTargetsTabController {

    static get UID() {
        return 'ngbTargetsTabController';
    }

    constructor(ngbTargetsTabService) {
        Object.assign(this, {ngbTargetsTabService});
    }

    get isTableMode() {
        return this.ngbTargetsTabService.isTableMode;
    }

    get isAddMode() {
        return this.ngbTargetsTabService.isAddMode;
    }

    get isEditMode() {
        return this.ngbTargetsTabService.isEditMode;
    }
}
