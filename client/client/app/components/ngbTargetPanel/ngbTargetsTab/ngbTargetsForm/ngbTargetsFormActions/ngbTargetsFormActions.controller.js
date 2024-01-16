export default class ngbTargetsFormActionsController {

    static get UID() {
        return 'ngbTargetsFormActionsController';
    }

    constructor(dispatcher, ngbTargetGenesTableService) {
        Object.assign(this, {dispatcher, ngbTargetGenesTableService});
    }

    get displayFilters() {
        return this.ngbTargetGenesTableService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbTargetGenesTableService.displayFilters = value;
    }

    async onChangeShowFilters() {
        await this.ngbTargetGenesTableService.onChangeShowFilters();
    }

    onClickRestore() {}

    onChangeColumn() {}
}
