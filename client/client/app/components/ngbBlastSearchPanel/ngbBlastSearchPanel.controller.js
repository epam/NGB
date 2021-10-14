import baseController from '../../shared/baseController';

export default class ngbBlastSearchPanelController extends baseController {

    blastStates;
    currentBlastState;
    tabSelected;

    static get UID() {
        return 'ngbBlastSearchPanelController';
    }

    constructor(dispatcher, $scope, $timeout, ngbBlastSearchService, ngbBlastHistoryTableService, $mdDialog) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            $scope,
            $timeout,
            ngbBlastHistoryTableService,
            ngbBlastSearchService,
            $mdDialog
        });
        this.blastStates = ngbBlastSearchService.blastStates;
        this.currentBlastState = this.blastStates.SEARCH;
        this.tabSelected = this.blastStates.SEARCH;
        this.initEvents();
    }

    events = {
        'read:show:blast': ::this.onExternalChange
    };

    changeState(state, isRepeat) {
        if (this.blastStates.hasOwnProperty(state)) {
            this.currentBlastState = this.blastStates[state];
            this.tabSelected = state === this.blastStates.SEARCH
                ? this.blastStates.SEARCH
                : this.blastStates.HISTORY;
        }
        this.ngbBlastSearchService.isRepeat = !!isRepeat;
        this.$timeout(::this.$scope.$apply);
    }

    clearHistory(event) {
        const confirm = this.$mdDialog.confirm()
            .title('Clear all history?')
            .ok('OK')
            .cancel('CANCEL');
        this.$mdDialog.show(confirm).then(::this.ngbBlastHistoryTableService.clearSearchHistory);
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
        const result = this.ngbBlastSearchService.cutCurrentResult;
        if (!result) {
            return;
        }
        this.ngbBlastHistoryTableService.downloadResults(result.id).then(data => {
            const linkElement = document.createElement('a');
            try {
                const blob = new Blob([data], {type: 'application/csv'});
                const url = window.URL.createObjectURL(blob);

                linkElement.setAttribute('href', url);
                // FIXME: dbName adjust
                linkElement.setAttribute('download',
                    `BLAST-${result.tool}-${result.dbName}-${result.title || result.id}.csv`);

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
        event.stopImmediatePropagation();
        return false;
    }

    onExternalChange(data) {
        if (this.currentBlastState !== this.blastStates.SEARCH) {
            this.ngbBlastSearchService.currentSearchId = null;
            this.ngbBlastSearchService.currentTool = data.tool;
            this.changeState('SEARCH');
        }
    }
}
