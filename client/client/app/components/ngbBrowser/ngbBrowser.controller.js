import  baseController from '../../shared/baseController';

export default class ngbBrowserController extends baseController {

    static get UID() {
        return 'ngbBrowserController';
    }

    projectContext;
    markdown;
    homePageUrl;

    constructor(dispatcher, projectContext, $scope, $timeout, $sce, ngbBrowserService) {
        super(dispatcher);
        Object.assign(this, {
            dispatcher,
            projectContext,
            ngbBrowserService,
            $scope,
            $timeout,
            $sce
        });
        this.initEvents();
        this.homePageUrl = this.projectContext.browserHomePageUrl;
    }

    events = {
        'chromosome:change': ::this.onStateChange,
        'reference:change': ::this.onStateChange,
        'reference:pre:change': ::this.onStateChange
    };

    $onInit() {
        this.onStateChange();
        this.getMarkdown();
    }

    onStateChange() {
        const reference = this.projectContext.reference;
        const chromosome = this.projectContext.currentChromosome;
        this.isChromosome = chromosome !== null;
        this.isProject = reference !== null && reference !== undefined;
        this.isProjectLoading = this.projectContext.referenceIsPromised;

        this.$timeout(::this.$scope.$apply);
    }

    browserHomePageUrl() {
        return this.$sce.trustAsResourceUrl(this.homePageUrl);
    }
    homePageUrlIsMd(){
        return this.homePageUrl && /.md$/.test(this.homePageUrl);
    }
    async getMarkdown() {
        if (!this.markdown && this.homePageUrlIsMd()) {
            this.markdown = await this.ngbBrowserService.getMarkdown(this.homePageUrl);
        }
    }
}
