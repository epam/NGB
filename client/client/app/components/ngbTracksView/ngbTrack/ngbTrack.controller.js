import {tracks as trackConstructors} from '../../../../modules/render/';
import ngbTrackEvents from './ngbTrack.events';

const DEFAULT_HEIGHT = 40;
const MAX_TRACK_NAME_LENGTH = 30;
export default class ngbTrackController {
    domElement = null;
    dispatcher = null;
    projectContext;

    static get UID() {
        return 'ngbTrackController';
    }

    isLoaded = false;

    get height() {
        return this.domElement ? this.domElement.offsetHeight : 0;
    }

    get instanceConstructor() {
        return trackConstructors[this.track.format];
    }

    isResizing = false;
    showTracksHeaders = true;

    scope;

    _trackIsVisible = true;

    minifiedMenuIsOpened = false;
    minifiedSubMenuIsOpened = false;
    minifiedMenuCloseTimeout = null;

    trackNameTruncated;

    constructor($scope, $element, $compile, $timeout, dispatcher, projectContext, projectDataService, genomeDataService, localDataService, bamDataService, appLayout) {
        this.scope = $scope;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.domElement = $element[0];
        this._localDataService = localDataService;
        this.$timeout = $timeout;
        this.trackRendererElement = $element.find('.md-track-renderer')[0];
        if (this.projectContext.collapsedTrackHeaders !== undefined) {
            this.showTracksHeaders = !this.projectContext.collapsedTrackHeaders;
        } else {
            this.showTracksHeaders = this._localDataService.getSettings().showTracksHeaders;
        }
        this.isReference = this.track.format === 'REFERENCE';
        this.isRuler = this.track.format === 'Ruler';

        this.ngbTrackEvents = new ngbTrackEvents(this.dispatcher, this.projectContext, $scope, $compile, projectDataService, genomeDataService, bamDataService, appLayout);

        const trackConstructor = trackConstructors[this.track.format];
        if (!trackConstructor) {
            this.isLoaded = true;
            return;
        }

        this.showHideTrackButton = !this.isReference;

        this.possibleTrackHeight =
            this.instanceConstructor.config instanceof Object
            && Number.isFinite(this.instanceConstructor.config.height)
                ? this.instanceConstructor.config.height
                : DEFAULT_HEIGHT;

        this.trackNameTruncated = this.track.name;
        if (this.trackNameTruncated.length > MAX_TRACK_NAME_LENGTH) {
            this.trackNameTruncated = `...${this.trackNameTruncated.substring(this.trackNameTruncated.length - MAX_TRACK_NAME_LENGTH)}`;
        }

        this.loadTrackInstance();

        this.trackDataIsLoading = false;
        this.trackInstance.trackDataLoadingStatusChanged = (status) => {
            this.trackDataIsLoading = status;
            this.$timeout(::$scope.$apply);
        };

        $scope.$on('resizeStart', () => {
            this.isResizing = true;
            this.heightBeforeResizeStarted = this.trackInstance.height;
            return false;
        });

        $scope.$on('resize', (e, delta) => {
            this.trackInstance.height = this.heightBeforeResizeStarted + delta;
            // Track.height is getter/setter, so we should proxy it
            this.track.height = this.trackInstance.height;
            return false;
        });

        $scope.$on('resizeEnd', () => {
            if (this.isResizing) {

                const tracksState = this.projectContext.tracksState || [];
                const [tracksSettings ]= tracksState.filter(m => m.bioDataItemId.toLowerCase() === this.track.name.toLowerCase() &&
                m.projectId.toLowerCase() === this.track.projectId.toLowerCase());

                if (!tracksSettings) {
                    tracksState.push({
                        bioDataItemId: this.track.name.toLowerCase(),
                        height: this.track.height
                    });
                } else {
                    tracksSettings.height = this.track.height;
                }
                this.projectContext.changeState({tracksState});
                this.isResizing = false;
            }
        });
        const self = this;
        const globalSettingsChangedHandler = (state) => {
            self.trackInstance.globalSettingsChanged(state);
            if (self.projectContext.collapsedTrackHeaders !== undefined) {
                self.showTracksHeaders = !self.projectContext.collapsedTrackHeaders;
            } else {
                self.showTracksHeaders = state.showTracksHeaders;
            }
        };
        const trackCoverageSettingsChangedHandler = (state) => {
            if (!state.cancel && ((state.data.applyToCurrentTrack && this.track.name === state.source) ||
                (state.data.applyToWIGTracks && this.track.format === 'WIG') ||
                (state.data.applyToBAMTracks && this.track.format === 'BAM'))) {
                this.trackInstance.coverageScaleSettingsChanged(state);
            } else if (state.cancel && state.source === this.track.name && this.trackInstance.trackHasCoverageSubTrack) {
                this.trackInstance.coverageScaleSettingsChanged(state);
            }
        };
        dispatcher.on('settings:change', globalSettingsChangedHandler);
        dispatcher.on('track:headers:changed', globalSettingsChangedHandler);
        dispatcher.on('tracks:coverage:manual:configure:done', trackCoverageSettingsChangedHandler);

        $scope.$on('$destroy', () => {
            if (this.trackInstance) {
                dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
                dispatcher.removeListener('track:headers:changed', globalSettingsChangedHandler);
                dispatcher.removeListener('tracks:coverage:manual:configure:done', trackCoverageSettingsChangedHandler);
                this.trackInstance.destructor();
            }
            if (this.trackInstance && this.trackInstance.domElement && this.trackInstance.domElement.parentNode) {
                this.trackInstance.domElement.parentNode.removeChild(this.trackInstance.domElement);
            }
        });
    }

    onTrackItemClick(sender, data, event) {
        switch (event.name.toLowerCase()) {
            case 'variant-request':
                this.ngbTrackEvents.variationRequest(this.trackInstance, data, this.track, event);
                break;
            case 'read-click-event':
                this.ngbTrackEvents.readClick(this.trackInstance, data, this.track, event);
                break;
            case 'feature-click':
                this.ngbTrackEvents.featureClick(this.trackInstance, data, this.track, event);
                break;
        }
    }

    get trackIsVisible() {
        return this._trackIsVisible;
    }

    changeTrackVisibility(visible) {
        if (visible !== this._trackIsVisible) {
            this._trackIsVisible = visible;
            this.scope.$apply();
        }
    }

    changeTrackHeight(newHeight) {
        this.trackInstance.height = newHeight;
        this.track.height = this.trackInstance.height;
    }

    loadTrackInstance() {
        const tracksState = this.projectContext.tracksState;
        let trackSettings = null;
        let state = null;
        let height = null;
        if (tracksState) {
            [trackSettings] = tracksState.filter(data => data.bioDataItemId.toLowerCase() === this.track.name.toLowerCase());
            if (trackSettings) {
                state = trackSettings.state;
                height = trackSettings.height;
            }
        }
        this.trackInstance = new this.instanceConstructor({
            dataItemClicked: ::this.onTrackItemClick,
            changeTrackVisibility: ::this.changeTrackVisibility,
            changeTrackHeight: ::this.changeTrackHeight,
            dispatcher: this.dispatcher,
            ...this.trackOpts,
            ...this._localDataService.getSettings(),
            ...this.track,
            restoredHeight: height,
            projectContext: this.projectContext,
            state: state,
            viewport: this.viewport
        });

        this.track.instance = this.trackInstance;
        this.track.height = this.trackInstance.height;

        if (this.trackInstance && this.trackInstance.domElement) {
            this.domElement.querySelector('.js-ngb-render-container-target')
                .appendChild(this.trackInstance.domElement);
        }

        this.isLoaded = true;
    }

    get isResizable() {
        if (this.trackInstance) {
            return this.trackInstance.trackIsResizable;
        }
        return false;
    }

    hideTrack(event) {
        const tracksState = this.projectContext.tracksState;
        const trackName = this.track.name.toLowerCase();
        const projectId = this.track.projectId.toLowerCase();
        const [trackState] = tracksState.filter(t => t.bioDataItemId.toLowerCase() === trackName && t.projectId.toLowerCase() === projectId);
        if (trackState) {
            const index = tracksState.indexOf(trackState);
            tracksState.splice(index, 1);
            const referenceName = this.projectContext.reference.name.toLowerCase();
            if (tracksState.filter(t => t.bioDataItemId.toLowerCase() !== referenceName).length === 0) {
                this.projectContext.changeState({reference: null});
            } else {
                this.projectContext.changeState({tracksState});
            }
        }
        event.stopImmediatePropagation();
        event.stopPropagation();
        return false;
    }

    get isMinifiedMenuButtonVisible() {
        return this.trackInstance !== undefined && !this.trackDataIsLoading &&
            (this.trackInstance.getSettings || this.trackInstance.actions);
    }

    openOrCloseMinifiedMenu() {
        this.minifiedMenuIsOpened = !this.minifiedMenuIsOpened;
        if (!this.minifiedMenuIsOpened) {
            this.minifiedSubMenuIsOpened = false;
        }
        this.clearCloseMinifiedMenuTimeout();
    }

    clearCloseMinifiedMenuTimeout() {
        if (this.minifiedMenuCloseTimeout) {
            clearTimeout(this.minifiedMenuCloseTimeout);
            this.minifiedMenuCloseTimeout = null;
        }
    }

    onMinifiedHeaderMouseOver() {
        this.clearCloseMinifiedMenuTimeout();
    }

    onMinifiedHeaderMouseLeave() {
        this.clearCloseMinifiedMenuTimeout();
        if (this.minifiedSubMenuIsOpened) {
            return;
        }
        const self = this;
        const fn = () => {
            self.minifiedMenuIsOpened = false;
            self.minifiedSubMenuIsOpened = false;
            self.scope.$apply();
        };
        const delayMs = 2000; // 2 seconds
        this.minifiedMenuCloseTimeout = setTimeout(fn, delayMs);
    }

    onHandleAction(ctrl) {
        ctrl.minifiedMenuIsOpened = false;
        ctrl.minifiedSubMenuIsOpened = false;
    }

    onMenuOpened(ctrl) {
        ctrl.minifiedSubMenuIsOpened = true;
        ctrl.clearCloseMinifiedMenuTimeout();
    }

    onMenuClosed(ctrl) {
        ctrl.minifiedSubMenuIsOpened = false;
        ctrl.onMinifiedHeaderMouseLeave();
    }
}
