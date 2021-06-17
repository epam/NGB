import BaseController from '../../shared/baseController';

export default class ngbProjectInfoPanelController extends BaseController {
    /**
     * @returns {string}
     */
    static get UID() {
        return 'ngbProjectInfoPanelController';
    }

    projectContext;
    isDescriptionFile = false;
    isProgressShown = true;
    selectedDatasetIds = new Set();
    oldDatasetIds = new Set();
    blobIframe = {};
    blobUrl;
    events = {
        'dataset:selection:change': ::this.manageDescription,
    };

    /**
     * @constructor
     */
    /** @ngInject */
    constructor(projectContext, $scope, $element, $timeout, dispatcher) {
        super();
        Object.assign(this, {
            projectContext, $scope, $element, $timeout, dispatcher
        });
        this.manageDescription();
        this.initEvents();
        this.$scope.$on('$destroy', () => {
            URL.revokeObjectURL(this.blobUrl);
        });
    }

    manageDescription() {
        this.oldDatasetIds = this.selectedDatasetIds;
        this.selectedDatasetIds = new Set(this.projectContext.datasets
            .filter(item => item.isProject && (item.__selected || item.__indeterminate))
            .map(item => item.id));
        if (this.isDiff(this.oldDatasetIds, this.selectedDatasetIds)) {
            if (this.selectedDatasetIds.size === 1) {
                this.isProgressShown = true;
                this.projectContext.loadDatasetDescription(this.selectedDatasetIds.values().next().value).then(data => {
                    if (data && data.byteLength) {
                        this.isDescriptionFile = true;
                        this.$scope.$apply();
                        this.blobUrl = URL.createObjectURL(new Blob([data], {type: 'text/html'}));
                        this.$timeout(() => {
                            this.blobIframe = this.$element.find('#description_file');
                            this.blobIframe[0].src = this.blobUrl;
                            this.isProgressShown = false;
                        }, 0);
                    } else {
                        this.isDescriptionFile = false;
                        this.isProgressShown = false;
                    }
                });
            } else {
                this.isDescriptionFile = false;
                this.isProgressShown = false;
            }
        }
    }

    get containsVcfFiles() {
        return this.projectContext.containsVcfFiles && !this.projectContext.variantsGroupError;
    }

    isDiff(set1, set2) {
        if (set1.size !== set2.size) {
            return true;
        }
        const _difference = new Set(set1);
        for (const elem of set2) {
            _difference.delete(elem);
        }
        return !!_difference.size;
    }
}
