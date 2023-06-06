export default class ngbTargetsFilterInput {

    prevValue;
    value;

    get filterInfo() {
        return this.ngbTargetsTableService.filterInfo || {};
    }

    static get UID() {
        return 'ngbTargetsFilterInput';
    }

    constructor($scope, dispatcher, ngbTargetsTableService) {
        Object.assign(this, {dispatcher, ngbTargetsTableService});
        this.prevValue = this.value = this.filterInfo[this.column.field];

        const resetFilters = this.resetFilters.bind(this);
        this.dispatcher.on('targets:filters:reset', resetFilters);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('targets:filters:reset', resetFilters);
        });
    }

    apply() {
        let shouldUpdate = false;
        let string = this.prevValue;
        if (this.prevValue !== this.value) {
            if (!this.value || !this.value.length) {
                this.value = null;
            }
            this.prevValue = this.value;
            shouldUpdate = true;
            string = this.value;
        }
        if (shouldUpdate) {
            this.ngbTargetsTableService.setFilter(this.column.field, string);
            this.dispatcher.emit('targets:filters:changed');
        }
    }

    resetFilters() {
        this.value = '';
        this.prevValue = '';
    }
}
