import  baseController from '../../shared/baseController';

export default class ngbBrowserController extends baseController {

    static get UID() {
        return 'ngbBrowserController';
    }

    projectContext;

    constructor(dispatcher, projectContext, $scope, $timeout, $sce) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            projectContext,
            $scope,
            $timeout,
            $sce
        });
        this.initEvents();
    }

    events = {
        'chromosome:change': this.onStateChange.bind(this),
        'reference:change': this.onStateChange.bind(this),
        'reference:pre:change': this.onStateChange.bind(this)
    };

    $onInit() {
        this.onStateChange();
    }

    onStateChange() {
        const reference = this.projectContext.reference;
        const chromosome = this.projectContext.currentChromosome;
        this.isChromosome = chromosome !== null;
        this.isProject = reference !== null && reference !== undefined;
        this.isProjectLoading = this.projectContext.referenceIsPromised;

        this.$timeout(() => {
            this.$scope.$apply();
        });
    }

    browserHomePageUrl() {
        return this.$sce.trustAsResourceUrl(this.projectContext.browserHomePageUrl);
    }
}