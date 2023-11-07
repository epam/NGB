export default class ngbStructurePanelController {

    static get UID() {
        return 'ngbStructurePanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbStructurePanelService, ngbTargetPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbStructurePanelService, ngbTargetPanelService});
    }

    get loadingData() {
        return this.ngbStructurePanelService.loadingData;
    }
    set loadingData(value) {
        this.ngbStructurePanelService.loadingData = value;
    }

    get sourceOptions () {
        return this.ngbStructurePanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbStructurePanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbStructurePanelService.sourceModel = value;
    }

    get pdbDescriptionLoading() {
        return this.ngbStructurePanelService.pdbDescriptionLoading;
    }
    get pdbDescriptionFailed() {
        return this.ngbStructurePanelService.pdbDescriptionFailed;
    }
    get pdbDescriptionErrorMessageList() {
        return this.ngbStructurePanelService.pdbDescriptionErrorMessageList;
    }
    get tableResults() {
        const results = this.ngbStructurePanelService.structureResults;
        return results && results.length;
    }

    get geneChips() {
        return [...this.ngbTargetPanelService.allChips];
    }

    onChangeSource() {
        this.dispatcher.emit('target:identification:structure:source:changed');
    }

    exportResults() {
        this.loadingData = true;
        this.ngbStructurePanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.geneChips.join('_')}-${this.sourceModel}-structures.csv`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.loadingData = false;
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                    this.loadingData = false;
                }
                this.$timeout(() => this.$scope.$apply());
            });
    }
}
