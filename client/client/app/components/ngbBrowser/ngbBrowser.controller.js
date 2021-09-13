import  baseController from '../../shared/baseController';
export default class ngbBrowserController extends baseController {

    static get UID() {
        return 'ngbBrowserController';
    }
    markdown;
    homeUrl;
    errorMessages=[];
    rootPageIsLoading=true;

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
        this.$scope.$watch('$ctrl.homeUrl', this.homeUrlChanged.bind(this));
        this.initEvents();
    }
    get homeUrl() {
        return this.projectContext && this.projectContext.browserHomePageUrl;
    }

    events = {
        'chromosome:change': ::this.onStateChange,
        'reference:change': ::this.onStateChange,
        'reference:pre:change': ::this.onStateChange,
        'defaultSettings:change': :: this.homeUrlChanged
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
        this.$timeout(::this.$scope.$apply);
    }

    browserHomePageUrl() {
        return this.$sce.trustAsResourceUrl(this.homeUrl);
    }

    homeUrlIsMd() {
        return this.homeUrl && /.md$/.test(this.homeUrl);
    }
    async homeUrlChanged() {
        this.rootPageIsLoading = true;
        if (this.homeUrl) {
            try {
                this.markdown = await this.ngbBrowserService.getMarkdown(this.homeUrl);
                this.errorMessages = [];
            } catch(err) {
                this.errorMessages.push(err.message);
            }
        }
        this.rootPageIsLoading = false;
        this.$timeout(::this.$scope.$apply);
    }
}
