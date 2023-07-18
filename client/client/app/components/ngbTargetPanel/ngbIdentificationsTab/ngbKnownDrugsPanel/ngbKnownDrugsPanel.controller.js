export default class ngbKnownDrugsPanelController {
    static get UID() {
        return 'ngbKnownDrugsPanelController';
    }

    constructor($scope, $timeout, dispatcher, ngbKnownDrugsPanelService) {
        Object.assign(this, {$scope, $timeout, dispatcher, ngbKnownDrugsPanelService});
    }

    get sourceOptions () {
        return this.ngbKnownDrugsPanelService.sourceOptions;
    }

    get sourceModel() {
        return this.ngbKnownDrugsPanelService.sourceModel;
    }
    set sourceModel(value) {
        this.ngbKnownDrugsPanelService.sourceModel = value;
    }

    get loading() {
        return this.ngbKnownDrugsPanelService
            ? this.ngbKnownDrugsPanelService.loading
            : false;
    }

    onChangeSource() {
        this.dispatcher.emit('target:identification:drugs:source:changed');
    }
}
