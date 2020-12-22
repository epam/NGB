import {scaleModes} from '../tracks/wig/modes';

class GroupAutoScaleManager {
    static instance (projectContext, dispatcher) {
        return new GroupAutoScaleManager(projectContext, dispatcher);
    }

    projectContext;
    groups = [];

    constructor(projectContext, dispatcher) {
        this.projectContext = projectContext;
        dispatcher.on('tracks:state:change', this.correctAutoScaleGroups);
        dispatcher.on('chromosome:change', this.correctAutoScaleGroups);
        this.correctAutoScaleGroups();
    }

    correctAutoScaleGroups = () => {
        const tracksState = this.projectContext.tracksState || [];
        const groups = [
            ...(new Set(
                (tracksState || [])
                    .map(t => t.state ? t.state.groupAutoScale : undefined)
                    .filter(Boolean)
                )
            )
        ];
        let changed = false;
        for (let g = 0; g < groups.length; g++) {
            const group = groups[g];
            const groupTracks = tracksState.filter(t => t.state && t.state.groupAutoScale === group);
            if (groupTracks.length < 2) {
                groupTracks.forEach(t => {
                    t.state.coverageScaleMode = scaleModes.defaultScaleMode;
                    t.state.groupAutoScale = undefined;
                });
                changed = true;
                const groupTracksInstances = (this.projectContext.trackInstances || [])
                    .filter(t => t.state.groupAutoScale === group);
                groupTracksInstances.forEach((groupTrack) => {
                    groupTrack.state.coverageScaleMode = scaleModes.defaultScaleMode;
                    groupTrack.state.groupAutoScale = undefined;
                    groupTrack._flags.dataChanged = true;
                    groupTrack.requestRender();
                });
                this.removeGroup(group);
            }
        }
        if (changed) {
            this.projectContext.changeState({tracksState});
        }
    };

    removeGroup(name) {
        const [group] = this.groups.filter(g => g.name === name);
        if (group) {
            const index = this.groups.indexOf(group);
            this.groups.splice(index, 1);
        }
    }

    renameGroup(oldName, newName) {
        const [group] = this.groups.filter(g => g.name === oldName);
        if (group) {
            group.name = newName;
        }
    }

    detachTrackData(groupName, track) {
        const [group] = this.groups.filter(g => g.name === name);
        if (group) {
            delete group.data[`${track.config.bioDataItemId}`];
        }
        this.reportTrackDataChangedForGroup();
    }

    getGroupData(name) {
        const [group] = this.groups.filter(g => g.name === name);
        if (group) {
            return Object.values(group.data || {});
        }
        return [];
    }

    getGroup(name) {
        const [group] = this.groups.filter(g => g.name === name);
        return group;
    }

    registerTrackData(track, data) {
        const {groupAutoScale: groupName, coverageScaleMode} = track.state;
        if (groupName && coverageScaleMode === scaleModes.groupAutoScaleMode) {
            let [group] = this.groups.filter(g => g.name === groupName);
            if (!group) {
                let index;
                const max = Math.max(...this.groups.map(group => group.index), -1);
                const notAvailable = new Set(this.groups.map(group => group.index));
                for (let i = 0; i < max; i++) {
                    if (!notAvailable.has(i)) {
                        index = i;
                        break;
                    }
                }
                if (index === undefined) {
                    index = max + 1;
                }
                group = {
                    name: groupName,
                    data: {},
                    index
                };
                this.groups.push(group);
            }
            group.data[`${track.config.bioDataItemId}`] = data;
            this.reportTrackDataChangedForGroup(groupName);
        }
    }

    unregisterTrack(track) {
        const group = track.state.groupAutoScale;
        if (group) {
            const groupTracks = (this.projectContext.trackInstances || [])
                .filter(t => t.state.groupAutoScale === group && t !== track);
            if (groupTracks.length < 2) {
                groupTracks.forEach((groupTrack) => {
                    groupTrack.state.coverageScaleMode = scaleModes.defaultScaleMode;
                    groupTrack.state.groupAutoScale = undefined;
                    groupTrack._flags.dataChanged = true;
                    groupTrack.requestRender();
                    groupTrack.reportTrackState();
                });
                this.removeGroup(group);
            } else {
                const newGroupName = groupTracks.map(t => t.config.bioDataItemId.toString()).join('-');
                this.renameGroup(group, newGroupName);
                groupTracks.forEach((groupTrack) => {
                    groupTrack.state.groupAutoScale = newGroupName;
                    groupTrack._flags.dataChanged = true;
                    groupTrack.requestRender();
                    groupTrack.reportTrackState();
                });
                this.detachTrackData(newGroupName);
            }
        }
        track.state.groupAutoScale = undefined;
        track._flags.dataChanged = true;
        track.requestRender();
    }

    reportTrackDataChangedForGroup(group) {
        const groupTracks = (this.projectContext.trackInstances || [])
            .filter(t => t && t.state && t.state.groupAutoScale === group);
        if (groupTracks.length > 0) {
            groupTracks.forEach(track => {
                track._flags.dataChanged = true;
                track.requestRender();
            });
        }
    }
}

export default GroupAutoScaleManager;
