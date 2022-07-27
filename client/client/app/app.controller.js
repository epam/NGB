import angular from 'angular';
import baseController from './shared/baseController';
import {dataServicesConfiguration} from '../dataServices';

export default class ngbAppController extends baseController {
    static get UID() {
        return 'ngbAppController';
    }

    dispatcher;
    projectContext;
    miewContext;
    heatmapContext;
    isAuthenticationInProgress;

    /* @ngInject */
    constructor(dispatcher,
                projectContext,
                miewContext,
                heatmapContext,
                eventHotkey,
                $stateParams,
                $rootScope,
                $scope,
                $state,
                $mdDialog,
                projectDataService,
                genomeDataService,
                localDataService,
                utilsDataService,
                apiService,
                appearanceContext) {
        super();
        Object.assign(this, {
            $scope,
            $state,
            $stateParams,
            $mdDialog,
            dispatcher,
            eventHotkey,
            genomeDataService,
            projectContext,
            miewContext,
            heatmapContext,
            projectDataService,
            utilsDataService,
            apiService,
            appearanceContext
        });
        const {auth} = this.$stateParams;
        dataServicesConfiguration.authenticationMode = auth;

        this.utilsDataService.checkSessionExpirationBehavior();

        this.dictionaryState = localDataService.getDictionary().State;

        this.initStateFromParams();

        this.initEvents();

        this.eventHotkey.init();

        this.toolbarVisibility = projectContext.toolbarVisibility;

        $rootScope.$on('$stateChangeSuccess', (evt, toState, toParams) => {
            this._changeStateFromParams(toParams);
        });

        if (window.addEventListener) {
            window.addEventListener('message', (event) => {
                if (!!event.data && typeof event.data === 'object' && !Array.isArray(event.data)) {
                    this._listener(event);
                }
            });
        }
        this.emitReady();
    }

    events = {
        'confirm:authentication:redirect': ::this.confirmAuthenticationRedirect,
        'route:change': ::this._goToState
    };

    _listener(event) {
        const callerId = event.data.callerId ? event.data.callerId : null;
        switch (event.data.method) {
            case 'loadDataSet': {
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
            }
            case 'navigateToCoordinate': {
                const coordinates = event.data.params && event.data.params.coordinates ? event.data.params.coordinates : null;
                this.apiService.navigateToCoordinate(coordinates).then((response) => {
                    this._apiResponse(response, callerId);
                });
                break;
            }
            case 'toggleSelectTrack':
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
            case 'loadTracks':
            case 'loadRegisteredTracks': {
                const registered = event.data.method === 'loadRegisteredTracks';
                const tracks = event.data.params && event.data.params.tracks ? event.data.params.tracks : null;
                if (tracks) {
                    this.apiService.loadTracks(event.data.params, registered).then((response) => {
                        this._apiResponse(response, callerId);
                    });
                } else {
                    this._apiResponse({
                        message: `Api error: loadTrack wrong params ${JSON.stringify(event.data.params)}: tracks array is missing`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            }
            case 'setGlobalSettings': {
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
            }
            case 'setTrackSettings': {
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
            }
            case 'setToken': {
                const token = event.data.params && event.data.params.token ? event.data.params.token : null;
                if (token) {
                    this._apiResponse(this.apiService.setToken(token), callerId);
                } else {
                    this._apiResponse({
                        message: `Api error: setToken wrong param ${JSON.stringify(event.data.params)}`,
                        isSuccessful: false
                    }, callerId);
                }
                break;
            }
            case 'setEmbedded': {
                const embedded = event.data.params && event.data.params.embedded !== undefined
                    ? !!(event.data.params.embedded)
                    : true;
                this._apiResponse(this.apiService.setEmbeddedMode(embedded), callerId);
                break;
            }
            case 'setControlsVisibility': {
                this._apiResponse(this.apiService.setControlsVisibility(event.data.params), callerId);
                break;
            }
            default:
                this._apiResponse({
                    message: 'Api error: No such method.',
                    isSuccessful: false,
                }, callerId);
        }
    }

    emitReady() {
        this._apiResponse({
            isSuccessful: true,
            message: 'ready',
        });
    }

    _apiResponse(params, callerId = null) {
        params.callerId = callerId;
        if (window.location !== window.parent.location) {
            window.parent.postMessage(params, '*');
        }
        if (window.opener) {
            window.opener.postMessage(params, '*');
        }
    }

    _changeStateFromParams(params) {
        const {
            referenceId,
            chromosome,
            end,
            rewrite,
            start,
            tracks,
            filterByGenome,
            collapsedTrackHeaders,
            miew,
            heatmap
        } = params;
        const position = start
            ? {end, start}
            : null;
        this.projectContext.rewriteLayout = true;
        this.projectContext.collapsedTrackHeaders = collapsedTrackHeaders;
        this.projectContext.datasetsFilter = filterByGenome;
        if (rewrite) {
            this.projectContext.rewriteLayout = this.dictionaryState.on.toLowerCase() === rewrite.toLowerCase();
        }
        this.miewContext.routeInfo = miew;
        this.heatmapContext.routeInfo = heatmap;
        const tracksState = tracks
            ? this.projectContext
                .convertTracksStateFromJson(tracks)
                .filter(track => track.format !== 'BLAST')
            : null;
        this.projectContext.changeState({
            chromosome: chromosome ? {name: chromosome} : null,
            position: (start && !end) ? start : null,
            reference: referenceId ? {name: referenceId} : null,
            tracksState,
            viewport: position,
            filterDatasets: filterByGenome ? filterByGenome : null
        });
    }

    initStateFromParams() {
        this._changeStateFromParams(this.$stateParams);

        const {toolbar, layout, bookmark, screenshot, embedded, controls, panels, hideMenu} = this.$stateParams;
        if (embedded) {
            this.appearanceContext.embedded = this.dictionaryState.on.toLowerCase() === embedded.toLowerCase();
        } else if (controls) {
            try {
                const controlsVisibility = JSON.parse(controls);
                const getValue = value => {
                    if (
                        [
                            this.dictionaryState.on.toLowerCase(),
                            this.dictionaryState.off.toLowerCase()
                        ].includes((value || '').toLowerCase())
                    ) {
                        return this.dictionaryState.on.toLowerCase() === value.toLowerCase();
                    }
                    return value;
                };
                this.appearanceContext.parse(
                    Object.entries(controlsVisibility || {})
                        .map(([key, value]) => ({[key]: getValue(value)}))
                        .reduce((r, c) => ({...r, ...c}), {})
                );
            } catch (e) {
                // eslint-disable-next-line
                console.warn(`Wrong buttons format: ${e.message}`);
            }
        }

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
        if (panels) {
            try {
                const panelsArray = JSON.parse(panels);
                if (Array.isArray(panelsArray)) {
                    this.appearanceContext.initialPanels = panelsArray.map(panel => panel.toLowerCase());
                }
            } catch (e) {
                // eslint-disable-next-line no-console
                console.warn(`Error parsing "panels" attribute: ${e.message}`);
            }
        }
        if (hideMenu !== undefined) {
            const menuHidden = `${hideMenu}`.toLowerCase() === 'true' ||
                this.dictionaryState.on.toLowerCase() === `${hideMenu}`.toLowerCase();
            this.projectContext.toolbarVisibility = !menuHidden;
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
            inherit: false,
            location: true
        };
        if (tracks) {
            state.tracks = tracks;
        }
        state.miew = this.miewContext.routeInfo;
        state.heatmap = this.heatmapContext.routeInfo;
        this.$state.go(this.$state.current.name, state, options);
    }

    async confirmAuthenticationRedirect() {
        if (this.isAuthenticationInProgress) {
            return;
        }
        this.isAuthenticationInProgress = true;
        this.$mdDialog.show({
            clickOutsideToClose: false,
            contentElement: '#authenticationConfirmDialog',
            parent: angular.element(document.body)
        });
    }

    redirectToAuthentication() {
        this.utilsDataService.authenticate();
        this.isAuthenticationInProgress = false;
    }
}
