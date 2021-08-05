export default class ngbFeatureInfoHistoryController {

    static get UID() {
        return 'ngbFeatureInfoHistoryController';
    }

    constructor($scope, ngbFeatureInfoPanelService) {
        Object.assign(this, {$scope, ngbFeatureInfoPanelService});
        this.data = [
            {
              "uid": "e8dd9d77-2342-4fa6-ac6f-e110a596d717",
              "itemId": 1,
              "itemType": "GENE",
              "actionType": "UPDATE",
              "datetime": "2021-08-03 15:31:46.347",
              "username": "User",
              "field": "source",
              "oldValue": "old",
              "newValue": "new"
            },
            {
              "uid": "e8dd9d77-2342-4fa6-ac6f-e110a596d717",
              "itemId": 1,
              "itemType": "GENE",
              "actionType": "CREATE",
              "datetime": "2021-08-03 15:31:46.347",
              "username": "User",
              "field": "gene_name",
              "newValue": "newGene"
            },
            {
              "uid": "e8dd9d77-2342-4fa6-ac6f-e110a596d717",
              "itemId": 1,
              "itemType": "GENE",
              "actionType": "DELETE",
              "datetime": "2021-08-03 15:31:46.347",
              "username": "User",
              "field": "gene_id",
              "oldValue": "GeneId"
            }
          ];
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
