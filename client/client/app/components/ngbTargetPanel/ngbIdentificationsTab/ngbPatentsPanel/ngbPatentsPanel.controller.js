const PATENT_TABS = {
    sequences: 'sequences',
    chemicals: 'chemicals',
};

export default class ngbPatentsPanelController {

    get patentTabs () {
        return PATENT_TABS;
    }

    _tabs = [];
    _selectedTab;

    get tabs() {
        return this._tabs;
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

    constructor($scope, $timeout, dispatcher, ngbSequencesPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbSequencesPanelService});
    }

    $onInit() {
        this._tabs = Object.values(this.patentTabs);
        this._selectedTab = this.patentTabs.sequences;
    }

    onChangeTab(tab) {
        this.selectedTab = tab;
    }

    getTabName(tab) {
        return this.patentTabs[tab] || tab;
    }
}
