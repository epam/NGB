import {SelectionEvents} from '../../shared/selectionContext';
import baseController from '../../shared/baseController';

export default class ngbTracksSelectionController extends baseController {
    static get UID() {
        return 'ngbTracksSelectionController';
    }

    projectContext;
    selectionContext;
    scope;
    _allSelected = false;
    _tracks = [];
    showTrackOriginalName = true;

    constructor(
        $scope,
        dispatcher,
        projectContext,
        selectionContext,
        trackNamingService,
        localDataService
    ) {
        super(dispatcher);
        this.projectContext = projectContext;
        this.selectionContext = selectionContext;
        this.trackNamingService = trackNamingService;
        this.localDataService = localDataService;
        this.scope = $scope;
        this.dispatcher = dispatcher;
        this.showTrackOriginalName = localDataService.getSettings().showTrackOriginalName;
        const reloadTracks = () => {
            this._tracks = (this.projectContext.tracks || []).map(track => ({
                selected: this.getTrackIsSelected(track),
                track
            }));
            this._allSelected = this.selectionContext.allSelected;
        };
        const globalSettingsChangedHandler = (state) => {
            this.showTrackOriginalName = state.showTrackOriginalName;
        };
        this.dispatcher.on('tracks:state:change', reloadTracks);
        this.dispatcher.on(SelectionEvents.changed, reloadTracks);
        this.dispatcher.on('settings:change', globalSettingsChangedHandler);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('tracks:state:change', reloadTracks);
            dispatcher.removeListener(SelectionEvents.changed, reloadTracks);
            dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
        });
    }

    openMenu($mdOpenMenu, $event) {
        $mdOpenMenu($event);
    }

    get tracks() {
        return this._tracks;
    }

    get allSelected() {
        return this._allSelected;
    }

    set allSelected(value) {
        this._allSelected = value;
        if (value) {
            this.selectAll();
        } else {
            this.clearSelection();
        }
    }

    selectAll() {
        this.selectionContext.selectAll();
    }

    clearSelection() {
        this.selectionContext.clearSelection();
    }

    getTrackIsSelected(track) {
        return this.selectionContext
            .getTrackIsSelected(track ? track.bioDataItemId : undefined);
    }

    setTrackIsSelected(track) {
        const [t] = this.tracks.filter(o => o.track.bioDataItemId === track.bioDataItemId);
        if (t) {
            this.selectionContext
                .setTrackIsSelected(track ? track.bioDataItemId : undefined, t.selected);
        }
    }

    getCustomName(track) {
        return this.trackNamingService.getCustomName(track);
    }

    trackHash(track) {
        return `[${track.bioDataItemId}][${track.projectId}]`;
    }
}
