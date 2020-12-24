export default class ngbTrackNamingService {

    static instance(dispatcher) {
        return new ngbTrackNamingService(dispatcher);
    }

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this.customNames = {};
        this.updateNamesFromLocalStorage();
    }

    checkTrackValidity(track) {
        return typeof track === 'object' && track !== null && track.bioDataItemId;
    }

    updateNamesFromLocalStorage() {
        if(!localStorage.getItem('custom-names')) {
            return localStorage
              .setItem('custom-names', JSON.stringify(this.customNames));
        }
        this.customNames = JSON.parse(localStorage.getItem('custom-names'));
    }

    nameChanged(track = {}) {
        const customName = this.getCustomName(track);
        if (this.checkTrackValidity(track) && customName && track.name) {
            return customName.toLowerCase() !== track.name.toLowerCase();
        }
        return false;
    }

    setCustomNames(names) {
        this.customNames = names;
        localStorage.setItem('custom-names', JSON.stringify(this.customNames));
        this.dispatcher.emitSimpleEvent('track:custom:name', this.customNames);
    }

    setCustomName(track, newName) {
        if (this.checkTrackValidity(track)) {
            const id = track.bioDataItemId.toString();
            const currentCustomName = this.customNames[id] || '';
            if (currentCustomName === newName.trim()) {
                return null;
            }
            this.customNames[id] = newName.trim();
            localStorage.setItem('custom-names', JSON.stringify(this.customNames));
            this.dispatcher.emitSimpleEvent('track:custom:name', this.customNames);
        }
    }

    getCustomName(track) {
        if (this.checkTrackValidity(track)) {
            const id = track.bioDataItemId.toString();
            return this.customNames[id] || '';
        }
        return '';
    }
}
