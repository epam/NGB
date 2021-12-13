class AppearanceContext {
    static instance(dispatcher) {
        return new AppearanceContext(dispatcher);
    }
    _closeAllTracks = false;
    _fitAllTracks = false;
    _organizeTracks = false;
    _genomeAnnotations = false;
    _projectInfoSections = false;
    _tracksSelection = false;
    _close = false;
    _maximise = false;
    _hideAll = false;

    get embedded () {
        return this._embedded;
    }

    set embedded (value) {
        if (value !== this._embedded) {
            this._embedded = value;
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded});
        }
    }
    get closeAllTracks () {
        return this._closeAllTracks;
    }

    set closeAllTracks (value) {
        if (value !== this._closeAllTracks) {
            this._closeAllTracks = value;
            console.log('Setting `closeAllTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, closeAllTracks: this._closeAllTracks});
        }
    }
    get fitAllTracks () {
        return this._fitAllTracks;
    }

    set fitAllTracks (value) {
        if (value !== this._fitAllTracks) {
            this._fitAllTracks = value;
            console.log('Setting `fitAllTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, fitAllTracks: this._fitAllTracks});
        }
    }
    get organizeTracks () {
        return this._organizeTracks;
    }

    set organizeTracks (value) {
        if (value !== this._organizeTracks) {
            this._organizeTracks = value;
            console.log('Setting `organizeTracks` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, organizeTracks: this._organizeTracks});
        }
    }
    get genomeAnnotations () {
        return this._genomeAnnotations;
    }

    set genomeAnnotations (value) {
        if (value !== this._genomeAnnotations) {
            this._genomeAnnotations = value;
            console.log('Setting `genomeAnnotations` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, genomeAnnotations: this._genomeAnnotations});
        }
    }
    get projectInfoSections () {
        return this._projectInfoSections;
    }

    set projectInfoSections (value) {
        if (value !== this._projectInfoSections) {
            this._projectInfoSections = value;
            console.log('Setting `projectInfoSections` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, projectInfoSections: this._projectInfoSections});
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
            console.log('Setting `maximise` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, maximise: this._maximise});
        }
    }
    get hideAll () {
        return this._hideAll;
    }

    set hideAll (value) {
        if (value !== this._hideAll) {
            this._hideAll = value;
            console.log('Setting `hideAll` mode to', !!value);
            this.dispatcher.emitSimpleEvent('appearance:changed', {embedded: this._embedded, hideAll: this._hideAll});
        }
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this._embedded = false;
    }
}

export default AppearanceContext;
