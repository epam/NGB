import SelectionContext, {SelectionEvents} from '../../../shared/selectionContext';

export default class ngbTracksSelectionMenuController {

    static get UID() {
        return 'ngbTracksSelectionMenuController';
    }

    projectContext;
    selectionContext;
    dispatcher;
    tracksMenu = {};

    constructor($scope, projectContext, selectionContext: SelectionContext, dispatcher) {
        this.projectContext = projectContext;
        this.selectionContext = selectionContext;
        this.dispatcher = dispatcher;
        this.dispatcher.on(SelectionEvents.changed, ::this.onTracksSelectionChanged);
        $scope.$on('$destroy', () => {
            this.dispatcher.removeListener(SelectionEvents.changed, ::this.onTracksSelectionChanged);
        });
    }

    onTracksSelectionChanged() {
        this.tracksMenu = {};
        this.types.forEach(type => {
            const tracks = this.selectedTracks
                .filter(track => track.format === type)
                .map(track => track.instance)
                .filter(Boolean);
            const [anyTrack] = tracks;
            if (anyTrack && anyTrack.constructor.Menu) {
                this.tracksMenu[type] = anyTrack.constructor.Menu.attach(...tracks);
            }
        });
    }

    get isVisible() {
        return this.selectionContext.selected.length > 1;
    }

    get allSelected() {
        return this.selectionContext.allSelected;
    }

    selectAll() {
        this.selectionContext.selectAll();
    }

    clearSelection() {
        this.selectionContext.clearSelection();
    }

    get selectedTracks() {
        return this.selectionContext.tracks;
    }

    get types() {
        const types = [
            ...(new Set(this.selectedTracks.map(track => track.format).filter(Boolean)))
        ];
        types.sort();
        return types;
    }

    getTracksByType(type) {
        return this.selectedTracks.filter(track => track.format === type);
    }
}
