import baseController from '../../shared/baseController';
import {SelectionEvents} from '../../shared/selectionContext';

export default class ngbGenomeAnnotationsController extends baseController{
    static get UID() {
        return 'ngbGenomeAnnotationsController';
    }

    projectContext;
    scope;
    showTrackOriginalName = true;

    constructor($scope, dispatcher, projectContext, trackNamingService, localDataService) {
        super(dispatcher);
        this.projectContext = projectContext;
        this.trackNamingService = trackNamingService;
        this.scope = $scope;
        this.localDataService = localDataService;
        this.showTrackOriginalName = localDataService.getSettings().showTrackOriginalName;
        this.annotationFiles = [
            {
                name: 'Human_genome.fa'
            },
            {
                name: 'Human_genome.bed'
            }
        ];
        const globalSettingsChangedHandler = (state) => {
            this.showTrackOriginalName = state.showTrackOriginalName;
        };
        this.dispatcher.on('settings:change', globalSettingsChangedHandler);
        // We must remove event listener when component is destroyed.
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('settings:change', globalSettingsChangedHandler);
        });
    }

    openMenu($mdOpenMenu, $event) {
        if (this.referenceContainsAnnotations) {
            $mdOpenMenu($event);
        }
    }

    get reference() {
        if (this.projectContext && this.projectContext.reference) {
            return this.projectContext.reference;
        }
        return {
            annotationFiles: null,
            name: null,
        };
    }

    get referenceContainsAnnotations() {
        if (this.projectContext && this.projectContext.reference) {
            return this.projectContext.reference.annotationFiles && this.projectContext.reference.annotationFiles.length;
        }
        return false;
    }

    getCustomName(file) {
        return this.trackNamingService.getCustomName(file);
    }

    onAnnotationFileChanged(file) {
        if (file.selected) {
            const state = this.projectContext.reference.annotationFiles;
            const sortFn = (file1, file2) => {
                if (file1.selected !== file2.selected) {
                    if (file1.selected) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
                const index1 = state.indexOf(file1.name.toLowerCase());
                const index2 = state.indexOf(file2.name.toLowerCase());
                if (file1.name.toLowerCase() === file2.name.toLowerCase()) {
                    return 0;
                }
                if (file1.name.toLowerCase() === file.name.toLowerCase()) {
                    return 1;
                }
                if (file2.name.toLowerCase() === file.name.toLowerCase()) {
                    return -1;
                }
                if (index1 < index2) {
                    return -1;
                } else if (index1 > index2) {
                    return 1;
                }
                return 0;
            };
            this.projectContext.reference.annotationFiles.sort(sortFn);
            const tracks = this.projectContext.tracks;
            file.projectId = '';
            file.isLocal = true;
            tracks.push(file);
            const tracksState = this.projectContext.tracksState;
            const savedState = this.projectContext.getTrackState((file.name || '').toLowerCase(), file.projectId);
            if (savedState) {
                tracksState.push(Object.assign(savedState,{
                    bioDataItemId: file.name,
                    format: file.format,
                    isLocal: true,
                    projectId: '',
                }));
            } else {
                tracksState.push({
                    bioDataItemId: file.name,
                    format: file.format,
                    isLocal: true,
                    projectId: '',
                });
            }
            this.projectContext.changeState({tracks, tracksState});
        } else {
            const tracks = this.projectContext.tracks;
            const [track] = tracks.filter(
              (t) =>
                t.name.toLowerCase() === file.name.toLowerCase() &&
                t.format.toLowerCase() === file.format.toLowerCase()
            );
            if (track) {
                const index = tracks.indexOf(track);
                tracks.splice(index, 1);
            }
            const tracksState = this.projectContext.tracksState;
            const [trackState] = tracksState.filter(
              (t) => 
                t.bioDataItemId.toLowerCase() === file.name.toLowerCase() &&
                t.format.toLowerCase() === file.format.toLowerCase()
            );
            if (trackState) {
                tracksState.splice(tracksState.indexOf(trackState), 1);
            }
            this.projectContext.changeState({tracks, tracksState});
        }
        this.projectContext.saveAnnotationFilesState();
    }
}