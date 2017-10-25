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
            window.addEventListener("message", (event) => {
                if (!!event.data && typeof event.data === 'object' && !Array.isArray(event.data)) {
                    this._listener(event);
                }
            });
        }
    }

    events = {
        'route:change': ::this._goToState
    };

    _listener(event) {
        const callerId = event.data.callerId ? event.data.callerId : null;
        switch (event.data.method) {
            case "loadDataSet":
                const id = parseInt(event.data.params && event.data.params.id ? event.data.params.id : null);
                const forceSwitchRef = event.data.params && event.data.params.forceSwitchRef ? event.data.params.forceSwitchRef : false;
                if (id) {
                    this.apiService.loadDataSet(id, forceSwitchRef).then((response) => {
                        this._apiResponse(response, callerId);
                    });
                } else {
                    this._apiResponse({
                        message: `Api error: loadDataSet wrong param ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            case "navigateToCoordinate":
                const coordinates = event.data.params && event.data.params.coordinates ? event.data.params.coordinates : null;
                this._apiResponse(this.apiService.navigateToCoordinate(coordinates), callerId);
                break;
            case "toggleSelectTrack":
                if (event.data.params && event.data.params.track) {
                    this.apiService.toggleSelectTrack(event.data.params).then((response) => {
                        this._apiResponse(response, callerId);
                    });
                } else {
                    this._apiResponse({
                        message: `Api error: loadTrack wrong params ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            case "loadTracks":
                const tracks = event.data.params && event.data.params.tracks ? event.data.params.tracks : null,
                    mode = event.data.params && event.data.params.mode ? event.data.params.mode : null;
                if (tracks && mode) {
                    this.apiService.loadTracks(event.data.params).then((response) => {
                        this._apiResponse(response, callerId);
                    });
                } else {
                    this._apiResponse({
                        message: `Api error: loadTrack wrong params ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            case "setGlobalSettings":
                const globalSettingsParams = event.data.params;
                if (globalSettingsParams) {
                    this._apiResponse(this.apiService.setGlobalSettings(globalSettingsParams), callerId);
                } else {
                    this._apiResponse({
                        message: `Api error: setGlobalSettings wrong param ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            case "setTrackSettings":
                const trackSettingParams = event.data.params;
                if (trackSettingParams) {
                    this._apiResponse(this.apiService.setTrackSettings(trackSettingParams), callerId);
                } else {
                    this._apiResponse({
                        message: `Api error: setTrackSettings wrong param ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            default:
                this._apiResponse({
                    message: 'Api error: No such method.',
                    isSuccessful: false,
                }, callerId);
        }
    }

    _apiResponse(params, callerId) {
        params.callerId = callerId;
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