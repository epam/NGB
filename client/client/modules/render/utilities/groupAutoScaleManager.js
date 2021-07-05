import {scaleModes} from '../tracks/wig/modes';

class GroupAutoScaleManager {
    static instance (projectContext, dispatcher) {
        return new GroupAutoScaleManager(projectContext, dispatcher);
    }

    projectContext;
    groups = [];

    constructor(projectContext, dispatcher) {
        this.projectContext = projectContext;
        dispatcher.on('tracks:instance:change', this.correctAutoScaleGroupsDebounced);
        this.correctAutoScaleGroups();
    }

    correctAutoScaleGroupsDebounced = () => {
        if (this.correctAutoScaleGroupsDebouncedTimer) {
            clearTimeout(this.correctAutoScaleGroupsDebouncedTimer);
            this.correctAutoScaleGroupsDebouncedTimer = undefined;
        }
        const debounceMs = 100;
        this.correctAutoScaleGroupsDebouncedTimer = setTimeout(
            () => this.correctAutoScaleGroups(),
            debounceMs
        );
    };

    correctAutoScaleGroups = () => {
        const browsers = [
            ...(new Set(
                    (this.projectContext.getAllTrackInstances() || [])
                        .map(t => t.config ? t.config.browserId : undefined)
                )
            )
        ];
        const correctForBrowser = (browser) => {
            const tracks = this.projectContext.getTrackInstances(browser) || [];
            const groups = [
                ...(new Set(
                        (tracks || [])
                            .map(t => t.state ? t.state.groupAutoScale : undefined)
                            .filter(Boolean)
                    )
                )
            ];
            for (let g = 0; g < groups.length; g++) {
                const group = groups[g];
                const [b, ...rest] = group.split('-');
                if (b !== (browser || 'default')) {
                    const newName = [browser || 'default', ...rest].join('-');
                    this.renameGroup(group, newName);
                    tracks
                        .filter(t => t.state && t.state.groupAutoScale === group)
                        .forEach(t => {
                            t.state.groupAutoScale = newName;
                        });
                    groups[g] = newName;
                }
            }
            for (let g = 0; g < groups.length; g++) {
                const group = groups[g];
                const groupTracks = tracks.filter(t => t.state && t.state.groupAutoScale === group);
                if (groupTracks.length < 2) {
                    groupTracks.forEach(t => {
                        t.state.coverageScaleMode = scaleModes.defaultScaleMode;
                        t.state.groupAutoScale = undefined;
                        t._flags.dataChanged = true;
                        if (t.config && typeof t.config.reloadScope === 'function') {
                            t.config.reloadScope();
                        }
                        t.reportTrackState();
                        t.requestRender();
                    });
                    this.removeGroup(group);
                }
            }
        };
        browsers.forEach(browser => correctForBrowser(browser));
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
        const [group] = this.groups.filter(g => g.name === groupName);
        if (group) {
            delete group.data[`${track.config.bioDataItemId}/${track.config.duplicateId || ''}`];
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
            group.data[`${track.config.bioDataItemId}/${track.config.duplicateId || ''}`] = data;
            this.reportTrackDataChangedForGroup(groupName);
        }
    }

    unregisterTrack(track) {
        const group = track.state.groupAutoScale;
        if (group) {
            const groupTracks = (this.projectContext.getTrackInstances(track.browserId) || [])
                .filter(t => t.state.groupAutoScale === group && t !== track);
            if (groupTracks.length < 2) {
                groupTracks.forEach((groupTrack) => {
                    groupTrack.state.coverageScaleMode = scaleModes.defaultScaleMode;
                    groupTrack.state.groupAutoScale = undefined;
                    groupTrack._flags.dataChanged = true;
                    groupTrack.reportTrackState();
                    groupTrack.requestRender();
                });
                this.removeGroup(group);
            } else {
                const newGroupName = [
                    track.browserId || 'default',
                    ...groupTracks.map(t => `${t.config.bioDataItemId.toString()}/${t.config.duplicateId || ''}`)
                ].join('-');
                this.renameGroup(group, newGroupName);
                groupTracks.forEach((groupTrack) => {
                    groupTrack.state.groupAutoScale = newGroupName;
                    groupTrack._flags.dataChanged = true;
                    groupTrack.reportTrackState();
                    groupTrack.requestRender();
                });
                this.detachTrackData(newGroupName);
            }
        }
        track.state.groupAutoScale = undefined;
        track._flags.dataChanged = true;
    }

    reportTrackDataChangedForGroup(group) {
        const groupTracks = (this.projectContext.getAllTrackInstances() || [])
            .filter(t => t && t.state && t.state.groupAutoScale === group);
        if (groupTracks.length > 0) {
            groupTracks.forEach(track => track._flags.dataChanged = true);
        }
    }
}

export default GroupAutoScaleManager;
