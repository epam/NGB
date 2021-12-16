class AppearanceContext {
    static instance(dispatcher) {
        return new AppearanceContext(dispatcher);
    }

    get embedded () {
        return this._embedded;
    }

    set embedded (value) {
        if (value !== this._embedded) {
            this._embedded = value;
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded});
        }
    }
    get closeTracks () {
        return this._closeTracks;
    }

    set closeTracks (value) {
        if (value !== this._closeTracks) {
            this._closeTracks = value;
            console.log('Setting `closeTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, closeTracks: this._closeTracks});
        }
    }
    get fitTracks () {
        return this._fitTracks;
    }

    set fitTracks (value) {
        if (value !== this._fitTracks) {
            this._fitTracks = value;
            console.log('Setting `fitTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, fitTracks: this._fitTracks});
        }
    }
    get arrayTracks () {
        return this._arrayTracks;
    }

    set arrayTracks (value) {
        if (value !== this._arrayTracks) {
            this._arrayTracks = value;
            console.log('Setting `arrayTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, arrayTracks: this._arrayTracks});
        }
    }
    get genomeAnnot () {
        return this._genomeAnnot;
    }

    set genomeAnnot (value) {
        if (value !== this._genomeAnnot) {
            this._genomeAnnot = value;
            console.log('Setting `genomeAnnotations` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, genomeAnnot: this._genomeAnnot});
        }
    }
    get projectInfo() {
        return this._projectInfo;
    }

    set projectInfo (value) {
        if (value !== this._projectInfo) {
            this._projectInfo = value;
            console.log('Setting `projectInfos` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, projectInfo: this._projectInfo});
        }
    }
    get tracksSelection () {
        return this._tracksSelection;
    }

    set tracksSelection (value) {
        if (value !== this._tracksSelection) {
            this._tracksSelection = value;
            console.log('Setting `tracksSelection` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, tracksSelection: this._tracksSelection});
        }
    }
    get close () {
        return this._close;
    }

    set close (value) {
        if (value !== this._close) {
            this._close = value;
            console.log('Setting `close` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, close: this._close});
        }
    }
    get maximise () {
        return this._maximise;
    }

    set maximise (value) {
        if (value !== this._maximise) {
            this._maximise = value;
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, maximise: this._maximise});
        }
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this._embedded = false;
        this._closeTracks = true;
        this._fitTracks = true;
        this._arrayTracks = true;
        this._genomeAnnot = true;
        this._projectInfo = true;
        this._tracksSelection = true;
        this._close = true;
        this._maximise = true;
    }
}

export default AppearanceContext;
