export default class ngbTargetsTableActionsController {

    static get UID() {
        return 'ngbTargetsTableActionsController';
    }

    constructor(dispatcher, ngbTargetsTableService) {
        Object.assign(this, {dispatcher, ngbTargetsTableService});
    }

    get displayFilters() {
        return this.ngbTargetsTableService.displayFilters;
    }
    set displayFilters(value) {
        this.ngbTargetsTableService.displayFilters = value;
    }

    async onChangeShowFilters() {
        await this.ngbTargetsTableService.onChangeShowFilters()
            .then((response) => {
                if (response) {
                    this.dispatcher.emitSimpleEvent('targets:filters:list');
                }
            });
    }
}
