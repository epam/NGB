import  baseController from '../../../baseController';

export default class ngbShareLinkMenuController extends baseController {
    static get UID() {
        return 'ngbShareLinkMenuController';
    }
    layoutState = false;
    mainToolbarVisible = false;
    mainToolbarVisibleValue = true;
    rewriteLayout = false;
    screenShotVisible = false;
    screenShotVisibleValue = true;
    panelsHeadersVisibleValue = true;
    alias = null;

    events = {};
    projectContext;
    utilsDataService;

    constructor(
        $scope,
        dispatcher,
        projectContext,
        localDataService,
        stateParamsService,
        utilsDataService,
        targetContext
    ) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            localDataService,
            projectContext,
            stateParamsService,
            utilsDataService,
            targetContext,
        });

        this.state = localDataService.getDictionary().State;

        $scope.$watch('$ctrl.layoutState', ::this.generateUrl);
        $scope.$watch('$ctrl.mainToolbarVisible', ::this.generateUrl);
        $scope.$watch('$ctrl.mainToolbarVisibleValue', ::this.generateUrl);
        $scope.$watch('$ctrl.rewriteLayout', ::this.generateUrl);
        $scope.$watch('$ctrl.screenShotVisible', ::this.generateUrl);
        $scope.$watch('$ctrl.screenShotVisibleValue', ::this.generateUrl);
        $scope.$watch('$ctrl.panelsHeadersVisibleValue', ::this.generateUrl);
        $scope.$watch('$ctrl.alias', ::this.generateUrl);

        this.generateUrl();
    }

    async generateUrl() {
        const layout = this.projectContext.layout;
        //TODO: replace indexer with a settings section name
        layout[0].hasHeaders = (+!this.panelsHeadersVisibleValue).toString();
        const tracksState = this.projectContext.tracksState;

        const stateParams = this.stateParamsService.getPathParams();

        if (this.rewriteLayout === false) {
            stateParams.rewrite = this.showState(this.rewrite);
        }
        if (this.layoutState === true) {
            stateParams.layout = JSON.stringify(layout);
        }
        if (this.mainToolbarVisible === true) {
            stateParams.toolbar = this.showState(this.mainToolbarVisibleValue);
        }
        if (this.screenShotVisible === true) {
            stateParams.screenshot = this.showState(this.screenShotVisibleValue);
        }
        if (tracksState) {
            stateParams.tracks = this.projectContext.convertTracksStateToJson(tracksState);
        }
        if (this.trackState === true) {
            stateParams.tracks = JSON.stringify(tracksState);
        }
        const targetState = this.targetContext.currentState;
        if (targetState) {
            stateParams.target = JSON.stringify(targetState);
        }
        const fullUrl = this.stateParamsService.createUrl(stateParams);
        this.url = await this.utilsDataService.generateShortUrl(fullUrl, this.alias);
    }

    showState(value) {
        return value === true ? this.state.on : this.state.off;
    }
}
