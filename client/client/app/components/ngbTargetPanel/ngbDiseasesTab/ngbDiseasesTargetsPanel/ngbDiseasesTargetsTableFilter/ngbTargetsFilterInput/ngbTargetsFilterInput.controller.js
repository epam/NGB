export default class ngbTargetsFilterInputController {
    prevValue;
    value;

    constructor($scope, dispatcher, ngbDiseasesTargetsPanelService) {
        this.dispatcher = dispatcher;
        this.ngbDiseasesTargetsPanelService = ngbDiseasesTargetsPanelService;
        this.prevValue = this.value = (this.ngbDiseasesTargetsPanelService.filterInfo || {})[this.column.field];

        this.dispatcher.on('target:diseases:targets:filters:reset', this.resetFilters.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:diseases:targets:filters:reset', this.resetFilters.bind(this));
        });
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

    resetFilters() {
        this.value = undefined;
        this.prevValue = undefined;
    }
}
