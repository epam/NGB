const PATENTS_TABS = {
    SEQUENCES: 'SEQUENCES',
    CHEMICALS: 'CHEMICALS',
};

export default class ngbPatentsPanelController {

    get patentsTabs () {
        return PATENTS_TABS;
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

    constructor($scope, $timeout) {
        Object.assign(this, {$scope, $timeout});
    }

    $onInit() {
        this._tabs = Object.values(this.patentsTabs);
        this._selectedTab = this.patentsTabs.SEQUENCES;
    }

    onChangeTab(tab) {
        this.selectedTab = tab;
    }

    getTabName(tab) {
        return this.patentsTabs[tab] || tab;
    }
}
