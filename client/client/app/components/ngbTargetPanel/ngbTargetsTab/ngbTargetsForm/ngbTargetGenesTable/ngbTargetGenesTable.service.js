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

    static instance (dispatcher, ngbTargetsTabService) {
        return new ngbTargetGenesTableService(dispatcher, ngbTargetsTabService);
    }

    constructor(dispatcher, ngbTargetsTabService) {
        Object.assign(this, {dispatcher, ngbTargetsTabService});
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
}
