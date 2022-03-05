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
            this.reportStateChange();
        }
    }

    get preventCloseLastTrack () {
        return this._preventCloseLastTrack;
    }

    set preventCloseLastTrack (value) {
        if (value !== this._preventCloseLastTrack) {
            this._preventCloseLastTrack = value;
            this.reportStateChange();
        }
    }

    get closeLastTrackIgnoreFormats () {
        return this._closeLastTrackIgnoreFormats || ['MOTIFS', 'BLAST'];
    }

    set closeLastTrackIgnoreFormats (value) {
        if (value !== this._closeLastTrackIgnoreFormats) {
            this._closeLastTrackIgnoreFormats = /^none$/i.test(value)
                ? []
                : value;
            this.reportStateChange();
        }
    }

    get closeTracks () {
        return this._closeTracks;
    }

    set closeTracks (value) {
        if (value !== this._closeTracks) {
            this._closeTracks = value;
            this.reportStateChange();
        }
    }

    get fitTracks () {
        return this._fitTracks;
    }

    set fitTracks (value) {
        if (value !== this._fitTracks) {
            this._fitTracks = value;
            this.reportStateChange();
        }
    }

    get organizeTracks () {
        return this._organizeTracks;
    }

    set organizeTracks (value) {
        if (value !== this._organizeTracks) {
            this._organizeTracks = value;
            this.reportStateChange();
        }
    }

    get genomeAnnotations () {
        return this._genomeAnnotations;
    }

    set genomeAnnotations (value) {
        if (value !== this._genomeAnnotations) {
            this._genomeAnnotations = value;
            this.reportStateChange();
        }
    }

    get projectInfo() {
        return this._projectInfo;
    }

    set projectInfo (value) {
        if (value !== this._projectInfo) {
            this._projectInfo = value;
            this.reportStateChange();
        }
    }

    get tracksSelection () {
        return this._tracksSelection;
    }

    set tracksSelection (value) {
        if (value !== this._tracksSelection) {
            this._tracksSelection = value;
            this.reportStateChange();
        }
    }

    get close () {
        return this._close;
    }

    set close (value) {
        if (value !== this._close) {
            this._close = value;
            this.reportStateChange();
        }
    }

    get maximise () {
        return this._maximise;
    }

    set maximise (value) {
        if (value !== this._maximise) {
            this._maximise = value;
            this.reportStateChange();
        }
    }

    get vcfColumns () {
        return this._vcfColumns;
    }

    set vcfColumns (value) {
        if (value !== this._vcfColumns) {
            this._vcfColumns = value;
            this.reportStateChange();
        }
    }

    get vcfDownload () {
        return this._vcfDownload;
    }

    set vcfDownload (value) {
        if (value !== this._vcfDownload) {
            this._vcfDownload = value;
            this.reportStateChange();
        }
    }

    get genesColumns () {
        return this._genesColumns;
    }

    set genesColumns (value) {
        if (value !== this._genesColumns) {
            this._genesColumns = value;
            this.reportStateChange();
        }
    }

    get genesDownload () {
        return this._genesDownload;
    }

    set genesDownload (value) {
        if (value !== this._genesDownload) {
            this._genesDownload = value;
            this.reportStateChange();
        }
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this._embedded = false;
        this._preventCloseLastTrack = false;
        this._closeTracks = true;
        this._fitTracks = true;
        this._organizeTracks = true;
        this._genomeAnnotations = true;
        this._projectInfo = true;
        this._tracksSelection = true;
        this._close = true;
        this._maximise = true;
        this._vcfColumns = true;
        this._vcfDownload = true;
        this._genesColumns = true;
        this._genesDownload = true;
    }

    reportStateChange () {
        this.dispatcher.emitSimpleEvent('appearance:changed', this);
    }

    parse (visibilityPayload) {
        if (visibilityPayload) {
            // eslint-disable-next-line
            console.log('Setting controls visibility:', visibilityPayload);
            const {
                close = !this.embedded && this.close,
                maximize = !this.embedded && this.maximise,
                fit = !this.embedded && this.fitTracks,
                organize = !this.embedded && this.organizeTracks,
                annotations = !this.embedded && this.genomeAnnotations,
                project = !this.embedded && this.projectInfo,
                selection = !this.embedded && this.tracksSelection,
                clear = !this.embedded && this.closeTracks,
                vcfcolumns = !this.embedded && this.vcfColumns,
                vcfdownload = !this.embedded && this.vcfDownload,
                genescolumns = !this.embedded && this.genesColumns,
                genesdownload = !this.embedded && this.genesDownload,
                closelasttrack = !(this.embedded && this.preventCloseLastTrack),
                closeLastTrack = closelasttrack,
                closelasttrackignoreformats,
                closeLastTrackIgnoreFormats = closelasttrackignoreformats
            } = visibilityPayload;
            this._embedded = false;
            this._preventCloseLastTrack = !closeLastTrack;
            this._closeLastTrackIgnoreFormats = closeLastTrackIgnoreFormats;
            this._closeTracks = clear;
            this._fitTracks = fit;
            this._organizeTracks = organize;
            this._genomeAnnotations = annotations;
            this._projectInfo = project;
            this._tracksSelection = selection;
            this._vcfColumns = vcfcolumns;
            this._vcfDownload = vcfdownload;
            this._genesColumns = genescolumns;
            this._genesDownload = genesdownload;
            this._close = close;
            this._maximise = maximize;
            this.reportStateChange();
        }
    }
}

export default AppearanceContext;
