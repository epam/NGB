export default class ngbTrackNamingService {

    static instance() {
        return new ngbTrackNamingService();
    }

    constructor() {
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

    nameChanged(track) {
        const customName = this.getCustomName(track);
        const trackName = track.name;
        if (this.checkTrackValidity(track) && customName && trackName) {
            return customName.toLowerCase() !== trackName.toLowerCase();
        }
        return false;
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
