export default class ngbDiseasesControllerBase {
    constructor($scope, $timeout, dispatcher) {
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.dispatcher = dispatcher;
        const diseasesSourceChanged = this.sourceChanged.bind(this);
        const drugsSourceChanged = this.drugsSourceChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('diseases:source:changed', diseasesSourceChanged);
        dispatcher.on('drugs:source:changed', drugsSourceChanged);
        dispatcher.on('diseases:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('diseases:source:changed', diseasesSourceChanged);
            dispatcher.removeListener('drugs:source:changed', drugsSourceChanged);
            dispatcher.removeListener('diseases:filters:changed', filterChanged);
        });
    }

    $onInit() {
        (this.initialize)();
    }

    async initialize() {
        // override
    }

    async sourceChanged() {
        // override
    }

    async drugsSourceChanged() {
        // override
    }

    async filterChanged() {
        // override
    }
}
