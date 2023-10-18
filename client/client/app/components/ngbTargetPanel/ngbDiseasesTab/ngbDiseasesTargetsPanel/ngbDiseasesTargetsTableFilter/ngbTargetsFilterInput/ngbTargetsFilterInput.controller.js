export default class ngbTargetsFilterInputController {
    prevValue;
    value;

    constructor(dispatcher, ngbDiseasesTargetsPanelService) {
        this.dispatcher = dispatcher;
        this.ngbDiseasesTargetsPanelService = ngbDiseasesTargetsPanelService;
        this.prevValue = this.value = this.ngbDiseasesTargetsPanelService.filterInfo[this.column.field];
    }

    static get UID() {
        return 'ngbTargetsFilterInputController';
    }

    apply() {
        if (this.prevValue !== this.value) {
            this.prevValue = this.value;
            this.ngbDiseasesTargetsPanelService.setFilter(this.column.field, this.value);
            this.dispatcher.emit('target:diseases:targets:filters:changed');
        }
    }
}
