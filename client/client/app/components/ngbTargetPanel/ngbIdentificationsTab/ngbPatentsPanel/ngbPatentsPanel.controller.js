import {GOOGLE_PATENTS_SOURCE, NCBI_SOURCE} from './ngbPatentsPanel.service';

const PATENT_TABS = {
    sequences: 'sequences',
    chemicals: 'chemicals',
    general: 'general'
};

const PATENT_TAB_NAMES = {
    [PATENT_TABS.general]: 'General',
    [PATENT_TABS.sequences]: 'Sequences',
    [PATENT_TABS.chemicals]: 'Chemicals'
}

export default class ngbPatentsPanelController {

    get patentTabs () {
        return PATENT_TABS;
    }

    _selectedTab;

    get tabs() {
        if (!this.sourceModel) {
            return [];
        }
        switch (this.sourceModel.name) {
            case GOOGLE_PATENTS_SOURCE:
                return [PATENT_TABS.general, PATENT_TABS.chemicals];
            case NCBI_SOURCE:
            default:
                return [PATENT_TABS.sequences, PATENT_TABS.chemicals];
        }
    }
    get selectedTab() {
        return this._selectedTab;
    }
    set selectedTab(selectedTab) {
        this._selectedTab = selectedTab;
    }

    static get UID() {
        return 'ngbPatentsPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbSequencesPanelService, ngbPatentsSequencesTabService, ngbPatentsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbSequencesPanelService, ngbPatentsSequencesTabService, ngbPatentsPanelService});
    }

    $onInit() {
        this._selectedTab = this.tabs[0];
        this.ngbPatentsSequencesTabService.getDefaultSettings();
    }

    get sourceOptions () {
        return this.ngbPatentsPanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbPatentsPanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbPatentsPanelService.sourceModel = value;
    }

    onChangeTab(tab) {
        this.selectedTab = tab;
    }

    onChangeSource() {
        this.dispatcher.emit('target:identification:patents:source:changed');
        if (!this.tabs.includes(this.selectedTab)) {
            this.onChangeTab(this.tabs[0]);
        }
    }

    getTabName(tab) {
        return PATENT_TAB_NAMES[tab] || tab;
    }
}
