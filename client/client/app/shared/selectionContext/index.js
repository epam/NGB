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
    selected = [];

    constructor(dispatcher, projectContext) {
        this.dispatcher = dispatcher;
        this.projectContext = projectContext;
        this.dispatcher.on('reference:change', ::this.clearSelection);
        this.dispatcher.on('chromosome:change', ::this.clearSelection);
        this.dispatcher.on('tracks:instance:change', ::this.onTracksStateChanged);
    }

    get tracks() {
        const set = new Set(this.selected.map(id => `${id}`));
        return (this.projectContext.trackInstances || [])
            .filter(track => set.has(`${track.config.bioDataItemId}`));
    }

    get allSelected() {
        const selected = new Set(this.selected.map(id => `${id}`));
        const tracks = (this.projectContext.trackInstances || [])
            .filter(track => track.config.format !== 'REFERENCE');
        for (let idx = 0; idx < tracks.length; idx++) {
            if (!selected.has(`${tracks[idx].config.bioDataItemId}`)) {
                return false;
            }
        }
        return tracks.length > 0;
    }

    selectAll() {
        this.selected = (this.projectContext.trackInstances || [])
            .filter(track => track.config.format !== 'REFERENCE')
            .map(t => t.config.bioDataItemId);
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, this.selected);
    }

    clearSelection() {
        this.selected = [];
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, this.selected);
    }

    onTracksStateChanged() {
        const actualBioDataItemIds = new Set(
            (this.projectContext.trackInstances || [])
                .filter(track => track.config.format !== 'REFERENCE')
                .map(t => `${t.config.bioDataItemId}`)
        );
        this.selected = this.selected.filter(t => actualBioDataItemIds.has(`${t}`));
        this.dispatcher.emitSimpleEvent(SelectionEvents.changed, this.selected);
    }

    setTrackIsSelected(bioDataItemId, selected) {
        if (!bioDataItemId) {
            return;
        }
        const isSelected = this.getTrackIsSelected(bioDataItemId);
        const modified = isSelected !== selected;
        if (isSelected && !selected) {
            const index = this.selected.findIndex(t => `${t}` === `${bioDataItemId}`);
            this.selected.splice(index, 1);
        } else if (!isSelected && selected) {
            this.selected.push(bioDataItemId);
        }
        if (modified) {
            this.dispatcher.emitSimpleEvent(SelectionEvents.changed, this.selected);
        }
    }

    getTrackIsSelected(bioDataItemId) {
        if (!bioDataItemId) {
            return false;
        }
        return this.selected.findIndex(t => `${t}` === `${bioDataItemId}`) >= 0;
    }
}