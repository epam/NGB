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

    constructor(dispatcher, ngbDiseasesPanelService) {
        this.dispatcher = dispatcher;
        this.ngbDiseasesPanelService = ngbDiseasesPanelService;
        this._tabs = [];
        this._sources = [];
        this._selectedTab = undefined;
    }

    $onInit() {
        this._tabs = Object.values(DiseasesTabs);
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
        return this._tabs;
    }

    get selectedTab() {
        return this._selectedTab;
    }

    set selectedTab(selectedTab) {
        this._selectedTab = selectedTab;
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

    getTabName(tab) {
        return DiseaseTabNames[tab] || tab;
    }

    getSourceName(source) {
        return SourceOptionNames[source] || source;
    }
}

export default NgbDiseasesPanelController;
