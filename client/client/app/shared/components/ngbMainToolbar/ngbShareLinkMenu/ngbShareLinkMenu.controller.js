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

    events = {};

    projectContext;

    constructor($scope, dispatcher, projectContext, localDataService, stateParamsService) {
        super(dispatcher);

        Object.assign(this, {
            $scope,
            localDataService,
            projectContext,
            stateParamsService
        });

        this.state = localDataService.getDictionary().State;
    }


    generatedUrl() {
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
        this.url = this.stateParamsService.createUrl(stateParams);
        return this.url;
    }

    showState(value) {
        return value === true ? this.state.on : this.state.off;
    }
}