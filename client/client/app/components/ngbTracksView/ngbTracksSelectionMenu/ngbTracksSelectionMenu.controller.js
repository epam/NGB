import SelectionContext, {SelectionEvents} from '../../../shared/selectionContext';
import {CommonMenu} from '../../../../modules/render/tracks/common/menu';

export default class ngbTracksSelectionMenuController {

    static get UID() {
        return 'ngbTracksSelectionMenuController';
    }

    projectContext;
    selectionContext;
    dispatcher;
    tracksMenu = {};
    commonMenu;

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
        if (this.types.length > 1) {
            this.commonMenu = CommonMenu.attach(
                this.selectedTracks,
                {
                    group: true,
                    selection: this.selectedTracks,
                    types: this.types.slice()
                });
        } else {
            this.commonMenu = undefined;
            if (this.types.length === 1) {
                const type = this.types[0];
                const tracks = this.selectedTracks
                    .filter(track => track.config.format === type);
                const [anyTrack] = tracks;
                if (anyTrack && anyTrack.constructor.Menu) {
                    this.tracksMenu[type] = anyTrack.constructor.Menu.attach(
                        tracks,
                        {
                            group: true,
                            selection: this.selectedTracks,
                            types: [type]
                        }
                    );
                }
            }
        }
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
            ...(new Set(this.selectedTracks.map(track => track.config.format).filter(Boolean)))
        ];
        types.sort();
        return types;
    }

    getTracksByType(type) {
        return this.selectedTracks.filter(track => track.config.format === type);
    }
}
