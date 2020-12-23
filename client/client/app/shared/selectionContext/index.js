const SelectionEvents = {
    changed: 'tracks:selection:changed'
};

export {SelectionEvents};

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
        const set = new Set(this.getSelected(browserId).map(id => `${id}`));
        return (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => set.has(`${track.config.bioDataItemId}`));
    }

    allSelected(browserId) {
        const selected = new Set(this.getSelected(browserId).map(id => `${id}`));
        const tracks = (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => track.config.format !== 'REFERENCE');
        for (let idx = 0; idx < tracks.length; idx++) {
            if (!selected.has(`${tracks[idx].config.bioDataItemId}`)) {
                return false;
            }
        }
        return tracks.length > 0;
    }

    selectAll(browserId) {
        const selected = (this.projectContext.getTrackInstances(browserId) || [])
            .filter(track => track.config.format !== 'REFERENCE')
            .map(t => t.config.bioDataItemId);
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
        const actualBioDataItemIds = new Set(
            (this.projectContext.getTrackInstances(browserId) || [])
                .filter(track => track.config.format !== 'REFERENCE')
                .map(t => `${t.config.bioDataItemId}`)
        );
        const selected = this.getSelected(browserId).filter(t => actualBioDataItemIds.has(`${t}`));
        this.setSelected(browserId, selected);
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, selected);
    }

    setTrackIsSelected(bioDataItemId, browserId, selected) {
        if (!bioDataItemId) {
            return;
        }
        const isSelected = this.getTrackIsSelected(bioDataItemId, browserId);
        const modified = isSelected !== selected;
        const selectedArray = this.getSelected(browserId);
        if (isSelected && !selected) {
            const index = selectedArray.findIndex(t => `${t}` === `${bioDataItemId}`);
            selectedArray.splice(index, 1);
            this.setSelected(browserId, selectedArray);
        } else if (!isSelected && selected) {
            selectedArray.push(bioDataItemId);
            this.setSelected(browserId, selectedArray);
        }
        if (modified) {
            this.dispatcher.emitSimpleEvent(SelectionEvents.changed, selectedArray);
        }
    }

    getTrackIsSelected(bioDataItemId, browserId) {
        if (!bioDataItemId) {
            return false;
        }
        return this.getSelected(browserId).findIndex(t => `${t}` === `${bioDataItemId}`) >= 0;
    }
}