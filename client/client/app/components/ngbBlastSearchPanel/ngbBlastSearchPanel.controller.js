import baseController from '../../shared/baseController';

export default class ngbBlastSearchPanelController extends baseController {

    blastStates;
    currentBlastState;
    tabSelected;

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor(ngbBlastSearchService, ngbBlastHistoryTableService) {
        super();
        Object.assign(this, {
            ngbBlastHistoryTableService,
            ngbBlastSearchService
        });
        this.blastStates = ngbBlastSearchService.blastStates;
        this.currentBlastState = this.blastStates.SEARCH;
        this.tabSelected = this.blastStates.SEARCH;
    }

    changeState(state) {
        if (this.blastStates.hasOwnProperty(state)) {
            this.currentBlastState = state;
            this.tabSelected = state === this.blastStates.SEARCH
                ? this.blastStates.SEARCH
                : this.blastStates.HISTORY;
        }
    }

    clearHistory(event) {
        const confirm = this.$mdDialog.confirm()
            .title('Clear all history?')
            .ok('OK')
            .cancel('CANCEL');
        this.$mdDialog.show(confirm).then(async () => {
            await this.ngbBlastHistoryTableService.clearSearchHistory();
            await this.loadData();
        });
        event.stopImmediatePropagation();
        return false;
    }

    editSearch(event) {
        this.ngbBlastSearchService.currentSearchId = this.ngbBlastSearchService.currentResultId;
        this.changeState(this.blastStates.SEARCH);
        event.stopImmediatePropagation();
        return false;
    }

    downloadResults(event) {
        event.stopImmediatePropagation();
        return false;
    }

}
