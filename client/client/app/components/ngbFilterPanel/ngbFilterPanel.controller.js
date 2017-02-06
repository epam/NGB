export default class ngbFilterController {
    static get UID() {
        return 'ngbFilterPanelController';
    }

    projectContext;

    /** @ngInject */
    constructor(dispatcher, projectContext, $scope) {
        this._dispatcher = dispatcher;
        this.projectContext = projectContext;

        const ngbInit = () => {
            this.INIT();
        };

        this._dispatcher.on('reference:change', ngbInit);

        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('reference:change', ngbInit);
        });

        this.INIT();
    }

    get isProjectSelected() {
        return this.projectContext.reference !== null;
    }

    INIT() {
        this.filterItems = [
            {
                name: 'Active VCF files',
                id: 'files',
                isOpen: true
            }, {
                name: 'Gene',
                id: 'genes',
                isOpen: true
            }, {
                name: 'Type of variant',
                id: 'variants',
                isOpen: true
            }, {
                name: 'Variant location',
                id: 'exons',
                isOpen: true
            }, {
                name: 'quality',
                id: 'quality',
                isOpen: true
            }, {
                name: 'VCF Advanced Filter',
                id: 'advanced',
                isOpen: false
            }
        ];
    }

    setDefault() {
        this.projectContext.clearVcfFilter();
    }

}
