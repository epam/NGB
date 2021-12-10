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
    embeddedMode = false;
    alias = null;

    events = {};
    projectContext;
    utilsDataService;

    constructor($scope, dispatcher, projectContext, localDataService, stateParamsService, utilsDataService) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            localDataService,
            projectContext,
            stateParamsService,
            utilsDataService
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
        $scope.$watch('$ctrl.embeddedMode', ::this.generateUrl);

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
        if (this.embeddedMode) {
            stateParams.embedded = this.showState(this.embeddedMode);
        }
        if (this.trackState === true) {
            stateParams.tracks = JSON.stringify(tracksState);
        }
        const fullUrl = this.stateParamsService.createUrl(stateParams);
        this.url = await this.utilsDataService.generateShortUrl(fullUrl, this.alias);
        console.log(this.embeddedMode, stateParams, this.url);
    }

    showState(value) {
        return value === true ? this.state.on : this.state.off;
    }
}
