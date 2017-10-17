import angular from 'angular';
import baseController from './shared/baseController';

export default class ngbAppController extends baseController {
    static get UID() {
        return 'ngbAppController';
    }

    dispatcher;
    projectContext;

    /* @ngInject */
    constructor(dispatcher,
                projectContext,
                eventHotkey,
                $stateParams,
                $rootScope,
                $scope,
                $state,
                projectDataService,
                genomeDataService,
                localDataService,
                apiService) {
        super();
        Object.assign(this, {
            $scope,
            $state,
            $stateParams,
            dispatcher,
            eventHotkey,
            genomeDataService,
            projectContext,
            projectDataService,
            apiService
        });
        this.dictionaryState = localDataService.getDictionary().State;

        this.initStateFromParams();

        this.initEvents();

        this.eventHotkey.init();

        this.toolbarVisibility = projectContext.toolbarVisibility;

        $rootScope.$on('$stateChangeSuccess', (evt, toState, toParams) => {
            this._changeStateFromParams(toParams);
        });

        if (window.addEventListener) {
            window.addEventListener("message", () => this._listener(event));
        } else {
            // IE8
            window.attachEvent("onmessage", () => this._listener(event));
        }
    }

    events = {
        'route:change': ::this._goToState
    };

    _listener(event) {
        switch (event.data.method) {
            case "loadDataSet":
                const id = event.data.params && event.data.params.id ? event.data.params.id : null;
                if (id) {
                    this._apiResponse(this.apiService.loadDataSet(id));
                } else {
                    console.log("Api error: loadDataSet wrong param" + event.data);
                    this._apiResponse({
                        message: 'Api error: loadDataSet wrong param' + event.data,
                        completedSuccessfully: false
                    });
                }
                break;
            case "navigateToCoordinate":
                const coordinates = event.data.params && event.data.params.coordinates ? event.data.params.coordinates : null;
                if (coordinates) {
                    this._apiResponse(this.apiService.navigateToCoordinate(coordinates));
                } else {
                    console.log('Api error: navigateToCoordinate wrong param' + event.data);
                    this._apiResponse({
                        message: 'Api error: navigateToCoordinate wrong param' + event.data,
                        completedSuccessfully: false
                    });
                }
                break;
            case "setGlobalSettings":
                const params = event.data.params;
                if (params) {
                    this._apiResponse(this.apiService.setGlobalSettings(params));
                } else {
                    console.log('Api error: setGlobalSettings wrong param' + event.data);
                    this._apiResponse({
                        message: 'Api error: setGlobalSettings wrong param' + event.data,
                        completedSuccessfully: false
                    });
                }
                break;
            default:
                console.log("Api error: No such method.");
                console.log(event.data);
        }
    }

    _apiResponse(params) {
        window.parent.postMessage(params, '*');
    }


    _changeStateFromParams(params) {
        const {referenceId, chromosome, end, rewrite, start, tracks, filterByGenome, collapsedTrackHeaders} = params;
        const position = start
            ? {end, start}
            : null;
        this.projectContext.rewriteLayout = true;
        this.projectContext.collapsedTrackHeaders = collapsedTrackHeaders;
        this.projectContext.datasetsFilter = filterByGenome;
        if (rewrite) {
            this.projectContext.rewriteLayout = this.dictionaryState.on.toLowerCase() === rewrite.toLowerCase();
        }
        this.projectContext.changeState({
            chromosome: chromosome ? {name: chromosome} : null,
            position: (start && !end) ? start : null,
            reference: referenceId ? {name: referenceId} : null,
            tracksState: tracks ? this.projectContext.convertTracksStateFromJson(tracks) : null,
            viewport: position,
            filterDatasets: filterByGenome ? filterByGenome : null
        });
    }

    initStateFromParams() {
        this._changeStateFromParams(this.$stateParams);

        const {toolbar, layout, bookmark, screenshot} = this.$stateParams;

        if (toolbar) {
            const toolbarVisibility = this.dictionaryState.on.toLowerCase() === toolbar.toLowerCase();
            this.projectContext.toolbarVisibility = toolbarVisibility;
        }

        if (bookmark) {
            const bookmarkVisibility = this.dictionaryState.on.toLowerCase() === bookmark.toLowerCase();
            this.projectContext.bookmarkVisibility = bookmarkVisibility;
        }

        if (layout) {
            this.projectContext.layout = JSON.parse(layout);
        }

        if (screenshot) {
            const screenShotVisibility = this.dictionaryState.on.toLowerCase() === screenshot.toLowerCase();
            this.projectContext.screenShotVisibility = screenShotVisibility;
        }
    }

    _goToState() {
        const chromosome = this.projectContext.currentChromosome ? this.projectContext.currentChromosome.name : null;
        const start = this.projectContext.viewport ? parseInt(this.projectContext.viewport.start) : null;
        const end = this.projectContext.viewport ? parseInt(this.projectContext.viewport.end) : null;
        const referenceId = this.projectContext.reference ? this.projectContext.reference.name : null;
        const tracks = this.projectContext.tracksState ? this.projectContext.convertTracksStateToJson(this.projectContext.tracksState) : null;
        const filterByGenome = this.projectContext.datasetsFilter ? this.projectContext.datasetsFilter : null;
        const collapsedTrackHeaders = this.projectContext.collapsedTrackHeaders;
        const state = {
            chromosome,
            end,
            referenceId,
            start,
            filterByGenome,
            collapsedTrackHeaders
        };
        const options = {
            notify: false,
            inherit: false
        };
        if (tracks) {
            state.tracks = tracks;
        }
        this.$state.go(this.$state.current.name, state, options);
    }
}