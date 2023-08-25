export default class ngbStructureTableFilterController {

    prevValue = '';
    value = '';

    static get UID() {
        return 'ngbStructureTableFilterController';
    }

    constructor($scope, dispatcher, ngbStructurePanelService) {
        Object.assign(this, {dispatcher, ngbStructurePanelService});
        this.prevValue = this.value = this.filterInfo[this.column.field];

        const resetFilters = this.resetFilters.bind(this);
        this.dispatcher.on('target:identification:structure:filters:reset', resetFilters);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener('target:identification:structure:filters:reset', resetFilters);
        });
    }

    get filterInfo() {
        return this.ngbStructurePanelService.filterInfo || {};
    }

    apply() {
        let shouldUpdate = false;
        let string = this.prevValue;
        if (this.prevValue !== this.value) {
            if (!this.value || !this.value.length) {
                this.value = '';
            }
            this.prevValue = this.value;
            shouldUpdate = true;
            string = this.value;
        }
        if (shouldUpdate) {
            this.ngbStructurePanelService.setFilter(this.column.field, string);
            this.dispatcher.emit('target:identification:structure:filters:changed');
        }
    }

    resetFilters() {
        this.value = '';
        this.prevValue = '';
    }
}
