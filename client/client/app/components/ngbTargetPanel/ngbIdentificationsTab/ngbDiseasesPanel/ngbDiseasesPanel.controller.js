import {
    DiseasesTabs,
    DiseaseTabNames,
    SourceOptions,
    SourceOptionNames
} from './ngbDiseasesPanel.service';

class NgbDiseasesPanelController {
    static get UID () {
        return 'ngbDiseasesPanelController';
    }

    constructor(
        $scope,
        $timeout,
        dispatcher,
        ngbDiseasesPanelService,
        ngbDiseasesTableService,
        ngbTargetPanelService
    ) {
        Object.assign(this, {
            $scope,
            $timeout,
            dispatcher,
            ngbDiseasesPanelService,
            ngbDiseasesTableService,
            ngbTargetPanelService
        });
        this._tabs = [];
        this._tabsOnlyTable = [];
        this._sources = [];
        this._selectedTab = undefined;
    }

    $onInit() {
        this._tabs = Object.values(DiseasesTabs);
        this._tabsOnlyTable = [DiseasesTabs.table];
        this._sources = Object.values(SourceOptions);
        this._selectedTab = DiseasesTabs.table;
    }

    get tableTab() {
        return DiseasesTabs.table;
    }

    get bubblesTab() {
        return DiseasesTabs.bubbles;
    }

    get graphTab() {
        return DiseasesTabs.graph;
    }

    get tabs() {
        return this.chartsVisible ? this._tabs : this._tabsOnlyTable;
    }

    get selectedTab() {
        return this._selectedTab;
    }

    set selectedTab(selectedTab) {
        this._selectedTab = selectedTab;
    }

    get chartsVisible() {
        return this.source === SourceOptions.OPEN_TARGETS;
    }

    onChangeTab(newTab) {
        this.selectedTab = newTab;
    }

    get source() {
        if (this.ngbDiseasesPanelService) {
            return this.ngbDiseasesPanelService.sourceModel;
        }
        return undefined;
    }

    set source(source) {
        if (this.ngbDiseasesPanelService) {
            this.ngbDiseasesPanelService.sourceModel = source;
            if (source !== SourceOptions.OPEN_TARGETS) {
                this.selectedTab = DiseasesTabs.table;
            }
        }
    }

    get sources() {
        return this._sources;
    }

    get tableLoading() {
        return this.ngbDiseasesPanelService
            ? this.ngbDiseasesPanelService.tableLoading
            : false;
    }

    get chartsLoading() {
        return this.ngbDiseasesPanelService
            ? this.ngbDiseasesPanelService.chartsLoading
            : false;
    }
    get exportLoading() {
        return this.ngbDiseasesPanelService
            ? this.ngbDiseasesPanelService.exportLoading
            : false;
    }
    set exportLoading(value) {
        this.ngbDiseasesPanelService.exportLoading = value;
    }

    get tableResults() {
        const results = this.ngbDiseasesTableService.diseasesResults;
        return results && results.length;
    }

    get geneChips() {
        return [...this.ngbTargetPanelService.allGenes.map(i => i.chip)];
    }

    getTabName(tab) {
        return DiseaseTabNames[tab] || tab;
    }

    getSourceName(source) {
        return SourceOptionNames[source] || source;
    }

    exportResults() {
        this.exportLoading = true;
        this.ngbDiseasesPanelService.exportResults()
            .then(data => {
                const linkElement = document.createElement('a');
                try {
                    const blob = new Blob([data], {type: 'application/csv'});
                    const url = window.URL.createObjectURL(blob);

                    linkElement.setAttribute('href', url);
                    linkElement.setAttribute('download',
                        `${this.geneChips.join('_')}-${this.source}-diseases.csv`);

                    const clickEvent = new MouseEvent('click', {
                        'view': window,
                        'bubbles': true,
                        'cancelable': false
                    });
                    linkElement.dispatchEvent(clickEvent);
                    this.exportLoading = false;
                } catch (ex) {
                    // eslint-disable-next-line no-console
                    console.error(ex);
                    this.exportLoading = false;
                }
                this.$timeout(() => this.$scope.$apply());
            });
    }
}

export default NgbDiseasesPanelController;
