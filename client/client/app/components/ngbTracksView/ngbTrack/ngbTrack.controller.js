import ngbTrackEvents from './ngbTrack.events';
import SelectionContext, {SelectionEvents} from '../../../shared/selectionContext';
import BLASTContext from '../../../shared/blastContext';
import {tracks as trackConstructors} from '../../../../modules/render/';

const DEFAULT_HEIGHT = 40;
const MAX_TRACK_NAME_LENGTH = 30;
const KEYS = {
    enter: 13,
    esc: 27,
};
export default class ngbTrackController {
    domElement = null;
    dispatcher = null;
    projectContext;
    groupAutoScaleManager;
    fcSourcesManager;
    selectionContext: SelectionContext;
    blastContext: BLASTContext;

    static get UID() {
        return 'ngbTrackController';
    }

    isLoaded = false;

    get height() {
        return this.domElement ? this.domElement.offsetHeight : 0;
    }

    get instanceConstructor() {
        if (/^vcf$/i.test(this.track.format) && this.track.multiSample) {
            return trackConstructors.MultiSampleVCF;
        }
        return trackConstructors[this.track.format];
    }

    isResizing = false;
    showTracksHeaders = true;
    showTrackOriginalName = true;

    scope;

    _trackIsVisible = true;

    minifiedMenuIsOpened = false;
    minifiedSubMenuIsOpened = false;
    minifiedMenuCloseTimeout = null;

    trackNameTruncated;
    browserId;

    constructor(
        $scope,
        $element,
        $compile,
        $timeout,
        dispatcher,
        projectContext,
        projectDataService,
        genomeDataService,
        localDataService,
        bamDataService,
        appLayout,
        selectionContext,
        trackNamingService,
        groupAutoScaleManager,
        fcSourcesManager,
        blastContext
    ) {
        this.trackNamingService = trackNamingService;
        this.scope = $scope;
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.selectionContext = selectionContext;
        this.blastContext = blastContext;
        this.groupAutoScaleManager = groupAutoScaleManager;
        this.fcSourcesManager = fcSourcesManager;
        this.domElement = $element[0];
        this._localDataService = localDataService;
        this.$timeout = $timeout;
        this.trackRendererElement = $element.find('.md-track-renderer')[0];
        if (this.projectContext.collapsedTrackHeaders !== undefined) {
            this.showTracksHeaders = !this.projectContext.collapsedTrackHeaders;
        } else {
            this.showTracksHeaders = this._localDataService.getSettings().showTracksHeaders;
        }
        this.showTrackOriginalName = this._localDataService.getSettings().showTrackOriginalName;
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

        this.customNameTemp = this.customName;
        this.nameInputVisible = false;
        this.trackName = this.track.name;
        this.trackNameTruncated = this.trackName;
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
            this.trackInstance.isResizing = true;
            return false;
        });

        $scope.$on('resize', (e, delta) => {
            this.trackInstance.height = this.heightBeforeResizeStarted + delta;
            // Track.height is getter/setter, so we should proxy it
            this.track.height = this.trackInstance.height;
            return false;
        });

        $scope.$on('resizeEnd', () => {
            this.trackInstance.isResizing = false;
            if (this.isResizing) {

                const tracksState = this.projectContext.tracksState || [];
                const [tracksSettings] = tracksState
                    .filter(m => m.bioDataItemId.toLowerCase() === this.track.name.toLowerCase() &&
                        m.projectId.toLowerCase() === this.track.projectId.toLowerCase() &&
                        `${m.duplicateId || ''}` === `${this.track.duplicateId || ''}`
                    );

                if (!tracksSettings) {
                    tracksState.push({
                        bioDataItemId: this.track.name.toLowerCase(),
                        height: this.track.height,
                        duplicateId: this.track.duplicateId
                    });
                } else {
                    tracksSettings.height = this.track.height;
                }
                if (!this.silentInteractions) {
                    this.projectContext.changeState({tracksState});
                }
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
            self.showTrackOriginalName = state.showTrackOriginalName;
        };
        const trackCoverageSettingsChangedHandler = (state) => {
            if (
                !state.cancel &&
                this.browserId === state.data.browserId &&
                (
                    (state.data.applyToCurrentTrack && state.sources.indexOf(this.track.name) >= 0) ||
                    (state.data.applyToWIGTracks && this.track.format === 'WIG') ||
                    (state.data.applyToFeatureCountsTracks && this.track.format === 'FEATURE_COUNTS') ||
                    (state.data.applyToBAMTracks && this.track.format === 'BAM')
                )
            ) {
                this.trackInstance.coverageScaleSettingsChanged(state, this.track.format);
            } else if (state.cancel && state.sources.indexOf(this.track.name) >= 0 && this.trackInstance.trackHasCoverageSubTrack) {
                this.trackInstance.coverageScaleSettingsChanged(state, this.track.format);
            }
        };

        const trackColorsChangedHandler = state => {
            if (
                this.browserId === state.data.browserId &&
                (
                    (state.sources || []).indexOf(this.track.name) >= 0 ||
                    (state.data.applyToAllTracks && /^(wig|bam)$/i.test(this.track.format)) ||
                    (state.data.applyToAlignmentTracks && /^bam$/i.test(this.track.format)) ||
                    (state.data.applyToWIGTracks && /^wig$/i.test(this.track.format))
                )
            ) {
                this.trackInstance.colorSettingsChanged(state);
            }
        };

        const bedTrackColorsChangedHandler = state => {
            if (
                this.browserId === state.data.browserId &&
                (
                    (state.sources || []).indexOf(this.track.name) >= 0 ||
                    (state.data.applyToBEDTracks && /^bed$/i.test(this.track.format))
                )
            ) {
                this.trackInstance.colorSettingsChanged(state);
            }
        };

        const trackSettingsChangedHandler = (params) => {
            if(self.trackInstance.trackSettingsChanged) {
                self.trackInstance.trackSettingsChanged(params);
                this.scope.$apply();
            }
        };

        const tracksSelectionChangedHandler = () => {};

        const trackHeaderStyleChangedHandler = (state) => {
            if (
                this.browserId === state.data.browserId &&
                (
                    (state.data.applyToAllTracks) ||
                    (state.data.applyToAllTracksOfType && state.data.type === this.track.format) ||
                    state.sources.indexOf(this.track.name) >= 0
                )
            ) {
                this.trackInstance.headerStyleSettingsChanged(state);
            }
        };

        dispatcher.on('settings:change', globalSettingsChangedHandler);
        dispatcher.on('track:headers:changed', globalSettingsChangedHandler);
        dispatcher.on('tracks:header:style:configure:done', trackHeaderStyleChangedHandler);
        dispatcher.on('tracks:coverage:manual:configure:done', trackCoverageSettingsChangedHandler);
        dispatcher.on('coverage:color:configure:done', trackColorsChangedHandler);
        dispatcher.on('bed:color:configure:done', bedTrackColorsChangedHandler);
        dispatcher.on('trackSettings:change', trackSettingsChangedHandler);
        dispatcher.on(SelectionEvents.changed, tracksSelectionChangedHandler);
        $scope.$on('$destroy', () => {
            if (this.trackInstance) {
                if (this.trackInstanceRegistered) {
                    this.projectContext.unregisterTrackInstance(this.trackInstance);
                }
                dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
                dispatcher.removeListener('track:headers:changed', globalSettingsChangedHandler);
                dispatcher.removeListener('tracks:coverage:manual:configure:done', trackCoverageSettingsChangedHandler);
                dispatcher.removeListener('coverage:color:configure:done', trackColorsChangedHandler);
                dispatcher.removeListener('bed:color:configure:done', bedTrackColorsChangedHandler);
                dispatcher.removeListener('trackSettings:change', trackSettingsChangedHandler);
                dispatcher.removeListener(SelectionEvents.changed, tracksSelectionChangedHandler);
                this.trackInstance.destructor();
            }
            if (this.trackInstance && this.trackInstance.domElement && this.trackInstance.domElement.parentNode) {
                this.trackInstance.domElement.parentNode.removeChild(this.trackInstance.domElement);
            }
        });
    }

    truncate (o) {
        let result = o || '';
        if (result.length > MAX_TRACK_NAME_LENGTH) {
            result = `${result.substring(0, MAX_TRACK_NAME_LENGTH)}...`;
        }
        return result;
    }

    get customName() {
        return this.trackNamingService.getCustomName(this.track);
    }

    get showFileNameHint() {
        return this.trackNamingService.nameChanged(this.track) && this.showTrackOriginalName;
    }

    setCustomName(newName) {
        this.showFileName = this.trackNamingService.nameChanged(this.track);
        this.trackNamingService.setCustomName(this.track, newName);
        return this.hideTrackNameInput();
    }

    resetCustomNameChanges() {
        this.customNameTemp = this.trackNamingService.getCustomName(this.track);
        return this.hideTrackNameInput();
    }

    showTrackNameInput() {
        return this.nameInputVisible = true;
    }

    hideTrackNameInput() {
        return this.nameInputVisible = false;
    }

    handleNameInputKeydown(event) {
        event.stopPropagation();
        event.stopImmediatePropagation();
        if (event.keyCode === KEYS.esc) {
            this.resetCustomNameChanges();
        }
        if (event.keyCode === KEYS.enter) {
            this.setCustomName(this.customNameTemp);
        }
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

    get trackHeaderStyle() {
        if (this.trackInstance) {
            const styles = {
                ...this.trackInstance.config.header,
                ...this.trackInstance.state.header,
            };
            return {
                'font-size': styles.fontSize || '12px',
            };
        }
    }

    changeTrackVisibility(visible) {
        if (visible !== this._trackIsVisible) {
            this._trackIsVisible = visible;
            this.scope.$apply();
        }
    }

    get trackIsSelected() {
        return this.selectionContext
            .getTrackIsSelected(this.track, this.browserId);
    }

    set trackIsSelected(value) {
        return this.selectionContext
            .setTrackIsSelected(this.track, this.browserId, value);
    }

    trackSelectionChanged(event) {
        const trackIsSelected = this.trackIsSelected;
        const {shiftKey, ctrlKey, metaKey} = event;
        if (shiftKey && (ctrlKey || metaKey)) {
            !trackIsSelected
                ? this.selectionContext.selectAll(this.browserId)
                : this.selectionContext.clearSelection(this.browserId);
        } else if (shiftKey) {
            this.selectionContext
                .setTracksOfTypeAreSelected(
                    this.track ? this.track.format : undefined,
                    this.browserId,
                    !trackIsSelected
                );
        } else {
            this.selectionContext
                .setTrackIsSelected(this.track, this.browserId, !trackIsSelected);
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
            [trackSettings] = tracksState
                .filter(data => data.bioDataItemId.toLowerCase() === this.track.name.toLowerCase() &&
                    `${data.duplicateId || ''}` === `${this.track.duplicateId || ''}`
                );
            if (trackSettings) {
                state = trackSettings.state;
                height = trackSettings.height;
            }
        }
        if (this.state) {
            state = Object.assign(state, this.state);
        }
        this.trackInstance = new this.instanceConstructor({
            browserId: this.browserId,
            changeTrackHeight: ::this.changeTrackHeight,
            changeTrackVisibility: ::this.changeTrackVisibility,
            dataItemClicked: ::this.onTrackItemClick,
            ...this.trackOpts,
            ...this._localDataService.getSettings(),
            ...this.track,
            dispatcher: this.dispatcher,
            groupAutoScaleManager: this.groupAutoScaleManager,
            fcSourcesManager: this.fcSourcesManager,
            projectContext: this.projectContext,
            blastContext: this.blastContext,
            reloadScope: () => this.scope.$apply(),
            restoredHeight: height,
            silentInteractions: this.silentInteractions,
            state: state,
            viewport: this.viewport
        });
        if (!state || !state.sashimi) {
            this.trackInstanceRegistered = true;
            this.projectContext.registerTrackInstance(this.trackInstance);
        }

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
        if (this.silentInteractions) {
            event.stopImmediatePropagation();
            event.stopPropagation();
            return false;
        }
        const tracksState = this.projectContext.tracksState;
        const trackName = this.track.name.toLowerCase();
        const projectId = this.track.projectId.toLowerCase();
        const duplicateId = `${this.track.duplicateId || ''}`;
        const [trackState] = tracksState
            .filter(t => t.bioDataItemId.toLowerCase() === trackName &&
                t.projectId.toLowerCase() === projectId &&
                `${t.duplicateId || ''}` === duplicateId
            );
        if (trackState) {
            const index = tracksState.indexOf(trackState);
            tracksState.splice(index, 1);
            this.projectContext.changeState({tracksState});
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
