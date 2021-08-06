const EMPTY_HISTORY = 'There was not any change.';

export default class ngbFeatureInfoHistoryController {

    static get UID() {
        return 'ngbFeatureInfoHistoryController';
    }

    emptyHistoryData = EMPTY_HISTORY;

    constructor($scope, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService});
    }

    get historyData () {
        const data = this.ngbFeatureInfoPanelService.historyData;
        if (data.length) {
            data.sort((a, b) => {
                return new Date(b.datetime.split(' ').join('T')) - new Date(a.datetime.split(' ').join('T'));
            });
        }
        return data;
    }

    get isHistoryLoading () {
        return this.ngbFeatureInfoPanelService.getHistoryInProgress;
    }

    get errorHistory () {
        return this.ngbFeatureInfoPanelService.historyError;
    }

    changeInfo (change) {
        const date = new Date(change.datetime.split(' ').join('T'));
        const month = date.toLocaleString('default', { month: 'long' });
        const time = date.toLocaleString('en-US', { hour: 'numeric', minute: 'numeric', hour12: true });
        const info = `Changes by ${change.username} on ${date.getDate()} ${month} ${date.getFullYear()}, ${time}`;
        return info;
    }
}
