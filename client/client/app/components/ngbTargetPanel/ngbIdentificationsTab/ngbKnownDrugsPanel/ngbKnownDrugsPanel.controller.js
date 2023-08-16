export default class ngbKnownDrugsPanelController {
    static get UID() {
        return 'ngbKnownDrugsPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbKnownDrugsPanelService,
        ngbDrugsTableService,
        ngbTargetPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbKnownDrugsPanelService,
            ngbDrugsTableService,
            ngbTargetPanelService
        });
    }

    get sourceOptions () {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbKnownDrugsPanelService.sourceModel = value;
    }

    get loading() {
        return this.ngbKnownDrugsPanelService
            ? this.ngbKnownDrugsPanelService.loading
            : false;
    }

    get tableResults() {
        const results = this.ngbDrugsTableService.drugsResults;
        return results && results.length;
    }

    get geneChips() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.chip)];
    }

    onChangeSource() {
        this.dispatcher.emit('target:identification:drugs:source:changed');
    }

    exportResults() {
        this.ngbKnownDrugsPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.geneChips.join('_')}-${this.sourceModel.name}-drugs.csv`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                }
            });
    }
}
