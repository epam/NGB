import baseController from '../../shared/baseController';

export default class ngbBlastSearchPanelController extends baseController {

    tabs = {
        HISTORY: 1,
        RESULT: 2,
        SEARCH: 0
    };
    tabSelectedIndex = 0;
    resultDisabled = true;

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor(ngbBlastSearchService) {
        super();
        Object.assign(this, {
            ngbBlastSearchService
        });

    }

    changeTab(tab) {
        if (this.tabs.hasOwnProperty(tab)) {
            if (tab === 'RESULT' && this.ngbBlastSearchService.currentResultId) {
                this.resultDisabled = false;
            }
            this.tabSelectedIndex = this.tabs[tab];
        }
    }
}
