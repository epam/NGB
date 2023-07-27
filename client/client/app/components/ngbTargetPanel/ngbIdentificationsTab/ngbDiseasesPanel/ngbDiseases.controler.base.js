export default class ngbDiseasesControllerBase {
    constructor($scope, $timeout, dispatcher) {
        this.$scope = $scope;
        this.$timeout = $timeout;
        this.dispatcher = dispatcher;
        const sourceChanged = this.sourceChanged.bind(this);
        const diseasesSourceChanged = this.sourceChanged.bind(this);
        const filterChanged = this.filterChanged.bind(this);
        dispatcher.on('target:identification:changed', sourceChanged);
        dispatcher.on('target:identification:diseases:source:changed', diseasesSourceChanged);
        dispatcher.on('target:identification:diseases:filters:changed', filterChanged);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('target:identification:changed', sourceChanged);
            dispatcher.removeListener('target:identification:diseases:source:changed', diseasesSourceChanged);
            dispatcher.removeListener('target:identification:diseases:filters:changed', filterChanged);
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

    async filterChanged() {
        // override
    }
}
