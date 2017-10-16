export default class ngbApiService {

    static instance($state, projectContext, ngbDataSetsService, $mdDialog) {
        return new ngbApiService($state, projectContext, ngbDataSetsService, $mdDialog);
    }

    projectContext;
    ngbDataSetsService;
    $mdDialog;

    constructor($state, projectContext, ngbDataSetsService, $mdDialog) {
        Object.assign(this, {$state, projectContext, ngbDataSetsService, $mdDialog});
    }

    async loadDataSet(id) {
        let datasets = this.projectContext.datasets;
        if (!datasets || datasets.length === 0) {
            this.projectContext.refreshDatasets().then(async () => {
                datasets = await this.ngbDataSetsService.getDatasets();
                const [item] = datasets.filter(m => m.id === id);
                this._selectDataset(item, true, datasets);
            })
        } else {
            const [item] = datasets.filter(m => m.id === id);
            this._selectDataset(item, true, datasets);
        }

    }

    _selectDataset(item, isSelected, tree) {
        const self = this;
        if (!this.ngbDataSetsService.checkSelectionAvailable(item, isSelected)) {
            const reference = this.ngbDataSetsService.getItemReference(item);
            this.ngbDataSetsService.deselectItem(item);
            const confirm = this.$mdDialog.confirm()
                .title(`Switch reference ${this.projectContext.reference ? this.projectContext.reference.name : ''}${reference ? ` to ${reference.name}` : ''}?`)
                .textContent('All opened tracks will be closed.')
                .ariaLabel('Change reference')
                .ok('OK')
                .cancel('Cancel');
            this.$mdDialog.show(confirm).then(function () {
                self.ngbDataSetsService.selectItem(item, isSelected, tree);
            }, function () {
            });
        } else {
            this.ngbDataSetsService.selectItem(item, isSelected, tree);
        }
    }
}