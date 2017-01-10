export default
class projectTracksController {
    static get UID() {
        return 'projectTracksController';
    }

    /** @ngInject */
    constructor(dispatcher, genomeDataService, ngbProjectService, projectDataService, $scope) {
        this._dispatcher = dispatcher;
        this._genomeDataService = genomeDataService;
        this._ngbProjectService = ngbProjectService;
        this._projectDataService = projectDataService;

        const onReferenceChange = () => {
            this.INIT();
        };
        const onProjectEdit = () => {
            const projectId = this.projectId;
            this._projectDataService.getProject(projectId)
                .then((project)=> {
                    this.selectedTrackList = project.items
                        .filter(item => item.format !== 'REFERENCE')
                        .map(item =>
                            ( {
                                bioDataItemId: item.bioDataItemId,
                                hidden: item.hidden
                            }));
                    this.emitTracksChangeEvent();
                });
        };

        this._dispatcher.on('ngbProject:reference:change', onReferenceChange);
        this._dispatcher.on('ngbProject:edit', onProjectEdit);

        this._dispatcher.emitGlobalEvent(this.emittedEvent, {});

        $scope.$on('$destroy', () => {
            this._dispatcher.removeListener('ngbProject:reference:change', onReferenceChange);
            this._dispatcher.removeListener('ngbProject:edit', onProjectEdit);
        });
    }

    INIT() {
        this.selectedTrackList = [];
    }

    getAllTracks() {
        return this._ngbProjectService.getAllTracks();
    }

    toggleTrack(item) {
        const track = {
                bioDataItemId: item.bioDataItemId,
                hidden: false
            },
            idx = this.selectedTrackListContains(item);

        if (idx > -1) {
            this.selectedTrackList.splice(idx, 1);
        } else {
            this.selectedTrackList.push(track);
        }
        this.emitTracksChangeEvent();
    }

    existsTrack(item) {
        const idx = this.selectedTrackListContains(item);
        return idx !== -1
            ? true
            : false;
    }

    selectedTrackListContains(item) {
        if (!this.selectedTrackList) {
            return -1;
        }
        for (let i = 0; i < this.selectedTrackList.length; i++) {
            if (this.selectedTrackList[i].bioDataItemId === item.bioDataItemId) {
                return i;
            }
        }
        return -1;
    }

    emitTracksChangeEvent() {
        this._ngbProjectService.tracksChange({selectedTrackList: this.selectedTrackList});
    }


}