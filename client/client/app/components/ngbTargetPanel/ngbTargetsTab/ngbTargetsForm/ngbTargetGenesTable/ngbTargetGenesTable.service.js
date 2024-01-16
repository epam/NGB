const COLUMN_LIST = [{
    name: 'geneId',
    displayName: 'Gene ID'
}, {
    name: 'geneName',
    displayName: 'Gene Name'
}, {
    name: 'taxId',
    displayName: 'Tax ID'
}, {
    name: 'speciesName',
    displayName: 'Species Name'
}, {
    name: 'priority',
    displayName: 'Priority'
}, {
    name: 'remove',
    displayName: ''
}];

export default class ngbTargetGenesTableService {

    get columnList() {
        return COLUMN_LIST;
    }

    _tableResults = null;
    _displayFilters = false;

    get displayFilters() {
        return this._displayFilters;
    }
    set displayFilters(value) {
        this._displayFilters = value;
    }

    static instance (dispatcher, ngbTargetsTabService, ngbTargetPanelService) {
        return new ngbTargetGenesTableService(dispatcher, ngbTargetsTabService, ngbTargetPanelService);
    }

    constructor(dispatcher, ngbTargetsTabService, ngbTargetPanelService) {
        Object.assign(this, {dispatcher, ngbTargetsTabService, ngbTargetPanelService});
        dispatcher.on('target:model:changed', this.resetTargetModel.bind(this));
    }

    get tableResults() {
        return this._tableResults;
    }

    set loading(value) {
        this.ngbTargetsTabService.formLoading = value;
    }

    getColumnList() {
        return this.columnList;
    }

    getTableResults() {
        return new Promise(resolve => {
            this._tableResults = this.ngbTargetsTabService.targetModel.genes;
            this.loading = false;
            resolve(true);
        });
    }

    resetTargetModel() {
        this._tableResults = null;
    }

    async onChangeShowFilters() {}
}
