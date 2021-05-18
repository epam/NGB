import baseController from '../../shared/baseController';

export default class ngbBlastSearchPanelController extends baseController {

    tabs = {
        HISTORY: 1,
        RESULT: 2,
        SEARCH: 0
    };
    tabSelectedIndex = 0;

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor() {
        super();
    }

    changeTab(tab) {
        if (this.tabs.hasOwnProperty(tab)) {
            this.tabSelectedIndex = this.tabs[tab];
        }
    }
}
