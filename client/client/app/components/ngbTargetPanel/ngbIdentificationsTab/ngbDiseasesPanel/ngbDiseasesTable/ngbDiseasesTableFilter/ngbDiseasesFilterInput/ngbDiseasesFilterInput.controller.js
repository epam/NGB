export default class ngbDiseasesFilterInputController {
    prevValue;
    value;

    constructor($scope, dispatcher, ngbDiseasesTableService) {
        this.dispatcher = dispatcher;
        this.ngbDiseasesTableService = ngbDiseasesTableService;
        this.prevValue = this.value = (this.ngbDiseasesTableService.filterInfo || {})[this.column.field];

        this.dispatcher.on('diseases:filters:reset', this.resetFilters.bind(this));
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('diseases:filters:reset', this.resetFilters.bind(this));
        });
    }

    static get UID() {
        return 'ngbDiseasesFilterInputController';
    }

    apply() {
        if (this.prevValue !== this.value) {
            this.prevValue = this.value;
            this.ngbDiseasesTableService.setFilter(this.column.field, this.value);
            this.dispatcher.emit('target:identification:diseases:filters:changed');
        }
    }

    resetFilters() {
        this.value = undefined;
        this.prevValue = undefined;
    }
}
