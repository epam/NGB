import {tracks as trackConstructors} from '../../../../modules/render/';
import ngbTrackEvents from './ngbTrack.events';

const DEFAULT_HEIGHT = 40;
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

    isResizable = false;
    isResizing = false;

    constructor($scope, $element, $compile, $timeout, dispatcher, projectContext, projectDataService, genomeDataService, localDataService, bamDataService, appLayout) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.domElement = $element[0];
        this._localDataService = localDataService;
        this.$timeout = $timeout;
        this.trackRendererElement = $element.find('.md-track-renderer')[0];

        this.isRuler = this.track.format === 'Ruler';

        this.ngbTrackEvents = new ngbTrackEvents(this.dispatcher, this.projectContext, $scope, $compile, projectDataService, genomeDataService, bamDataService, appLayout);

        const trackConstructor = trackConstructors[this.track.format];
        if (!trackConstructor) {
            this.isLoaded = true;
            return;
        }

        this.showHideTrackButton = this.track.format != 'REFERENCE';

        this.possibleTrackHeight =
            this.instanceConstructor.config instanceof Object
            && Number.isFinite(this.instanceConstructor.config.height)
                ? this.instanceConstructor.config.height
                : DEFAULT_HEIGHT;

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
                const [tracksSettings ]= tracksState.filter(m => m.bioDataItemId === this.track.bioDataItemId);

                if (!tracksSettings) {
                    tracksState.push({
                        bioDataItemId: this.track.bioDataItemId,
                        height: this.track.height
                    });
                } else {
                    tracksSettings.height = this.track.height;
                }
                this.projectContext.changeState({tracksState});
                this.isResizing = false;
            }
        });

        const globalSettingsChangedHandler = ::this.trackInstance.globalSettingsChanged;
        dispatcher.on('settings:change', globalSettingsChangedHandler);

        $scope.$on('$destroy', () => {
            if (this.trackInstance) {
                dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
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

    loadTrackInstance() {
        const tracksState = this.projectContext.tracksState;
        let trackSettings = null;
        let state = null;
        let height = null;
        if (tracksState) {
            [trackSettings] = tracksState.filter(data => data.bioDataItemId === this.track.bioDataItemId);
            if (trackSettings) {
                state = trackSettings.state;
                height = trackSettings.height;
            }
        }
        this.trackInstance = new this.instanceConstructor({
            dataItemClicked: ::this.onTrackItemClick,
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
        this.isResizable = this.trackInstance.config.resizable !== false;

        if (this.trackInstance && this.trackInstance.domElement) {
            this.domElement.querySelector('.js-ngb-render-container-target')
                .appendChild(this.trackInstance.domElement);
        }

        this.isLoaded = true;
    }

    hideTrack(event) {
        const tracksState = this.projectContext.tracksState;
        const bioDataItemId = this.track.bioDataItemId;
        const projectId = this.track.projectId;
        const [trackState] = tracksState.filter(t => t.bioDataItemId === bioDataItemId && t.projectId === projectId);
        if (trackState) {
            const index = tracksState.indexOf(trackState);
            tracksState.splice(index, 1);
            const referenceId = this.projectContext.reference.bioDataItemId;
            if (tracksState.filter(t => t.bioDataItemId !== referenceId).length === 0) {
                this.projectContext.changeState({reference: null});
            } else {
                this.projectContext.changeState({tracksState});
            }
        }
        event.stopImmediatePropagation();
        event.stopPropagation();
        return false;
    }
}
