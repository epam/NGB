export default class ngbDiseasesTargetsTableFilterController {

    isString = false;
    isList = false;

    static get UID() {
        return 'ngbDiseasesTargetsTableFilterController';
    }

    constructor($scope, $element, dispatcher, ngbDiseasesTargetsPanelService) {
        Object.assign(this, {$scope, dispatcher, ngbDiseasesTargetsPanelService});

        switch (this.column.field) {
            case 'homologues': {
                this.isList = true;
                break;
            }
            default:
                this.isString = true;
                break;
        }
    }
}
