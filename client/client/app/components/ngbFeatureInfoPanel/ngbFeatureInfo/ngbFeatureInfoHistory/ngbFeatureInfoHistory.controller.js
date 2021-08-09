const EMPTY_HISTORY = 'Nothing changed';

export default class ngbFeatureInfoHistoryController {

    static get UID() {
        return 'ngbFeatureInfoHistoryController';
    }

    emptyHistoryData = EMPTY_HISTORY;

    constructor($scope, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService});
    }

    get historyData () {
        return this.ngbFeatureInfoPanelService.historyData;
    }

    get isHistoryLoading () {
        return this.ngbFeatureInfoPanelService.getHistoryInProgress;
    }

    get errorHistory () {
        return this.ngbFeatureInfoPanelService.historyError;
    }

    changeAuthor (change) {
        return !change || !change.username || /^anonymousUser$/i.test(change.username)
            ? 'anonymous user'
            : change.username;
    }

    changeDate (change) {
        return !change || !change.date ? 'N/A' : change.date;
    }
}
