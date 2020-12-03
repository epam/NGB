export default class ngbTrackNamingService {

    static instance($state, projectContext) {
        return new ngbTrackNamingService($state, projectContext);
    }

    constructor($state, projectContext) {
        this.projectContext = projectContext;
    }

    nameChanged(track) {
        const prettyName = this.getPrettyName(track) || '';
        return prettyName.toLowerCase() !== this.getTrackName(track).toLowerCase();
    }

    getTrackState(track) {
        const [trackState] = (this.projectContext.tracksState || [])
          .filter(m => m.bioDataItemId.toLowerCase() === track.name.toLowerCase() && m.projectId.toLowerCase() === track.projectId.toLowerCase());
        return trackState;
    }

    getTrackName(track) {
        if (track.isLocal) {
            const fileName = track.name;
            if (!fileName || !fileName.length) {
                return null;
            }
            let list = fileName.split('/');
            list = list[list.length - 1].split('\\');
            return list[list.length - 1];
        }
        return track.name;
    }

    getPrettyName(track) {
        return this.getTrackState(track).prettyName || this.getTrackName(track);
    }

    setPrettyName(newName, track) {
        const tracksState = this.projectContext.tracksState || [];
        const [tracksSettings] = tracksState
          .filter(m => m.bioDataItemId.toLowerCase() === track.name.toLowerCase() && m.projectId.toLowerCase() === track.projectId.toLowerCase());
        if (!tracksSettings) {
            tracksState.push({
                prettyName: newName,
            });
        } else {
            tracksSettings.prettyName = newName;
        }
        this.projectContext.changeState({tracksState});
    }
}
