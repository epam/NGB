const SelectionEvents = {
    changed: 'tracks:selection:changed'
};

export {SelectionEvents};

function getIdentifier (track) {
    return `${track.bioDataItemId}-${track.duplicateId || ''}`;
}

export default class SelectionContext {
    static instance(dispatcher, projectContext) {
        return new SelectionContext(dispatcher, projectContext);
    }

    dispatcher;
    projectContext;
    selected = {};

    constructor(dispatcher, projectContext) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.dispatcher.on('reference:change', () => this.clearSelection());
        this.dispatcher.on('chromosome:change', () => this.clearSelection());
        this.dispatcher.on('tracks:instance:change', () => this.onTracksStateChanged());
    }

    getSelected(browserId) {
        const id = browserId || 'default';
        return this.selected[id] || [];
    }

    setSelected(browserId, selected) {
        const id = browserId || 'default';
        this.selected[id] = selected || [];
    }

    getTracks(browserId) {
        const set = new Set(this.getSelected(browserId).map(getIdentifier));
        return (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => set.has(getIdentifier(track.config)));
    }

    allSelected(browserId) {
        const selected = new Set(this.getSelected(browserId).map(getIdentifier));
        const tracks = (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => track.config.format !== 'REFERENCE');
        for (let idx = 0; idx < tracks.length; idx++) {
            if (!selected.has(getIdentifier(tracks[idx].config))) {
                return false;
            }
        }
        return tracks.length > 0;
    }

    selectAll(browserId) {
        const selected = (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => track.config.format !== 'REFERENCE')
            .map(track => track.config);
        this.setSelected(browserId, selected);
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, selected);
    }

    clearSelection(browserId) {
        this.setSelected(browserId, []);
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, []);
    }

    onTracksStateChanged() {
        const browsers = Object.keys(this.selected);
        browsers.forEach(browser => this.onTracksStateChangedForBrowser(browser));
    }

    onTracksStateChangedForBrowser(browserId) {
        const actualIds = new Set(
            (this.projectContext.getTrackInstances(browserId) || [])
                .filter(track => track.config.format !== 'REFERENCE')
                .map(track => getIdentifier(track.config))
        );
        const selected = this.getSelected(browserId).filter(t => actualIds.has(getIdentifier(t)));
        this.setSelected(browserId, selected);
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, selected);
    }

    setTrackIsSelected(track, browserId, selected) {
        if (!track) {
            return;
        }
        const isSelected = this.getTrackIsSelected(track, browserId);
        const modified = isSelected !== selected;
        const selectedArray = this.getSelected(browserId);
        if (isSelected && !selected) {
            const index = selectedArray.findIndex(t => getIdentifier(t) === getIdentifier(track));
            selectedArray.splice(index, 1);
            this.setSelected(browserId, selectedArray);
        } else if (!isSelected && selected) {
            selectedArray.push({...track});
            this.setSelected(browserId, selectedArray);
        }
        if (modified) {
            this.dispatcher.emitSimpleEvent(SelectionEvents.changed, selectedArray);
        }
    }

    getTrackIsSelected(track, browserId) {
        if (!track) {
            return false;
        }
        return this.getSelected(browserId).findIndex(t => getIdentifier(t) === getIdentifier(track)) >= 0;
    }
}
