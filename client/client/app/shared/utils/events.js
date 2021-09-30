export class EventVariationInfo {
    constructor(obj) {
        const {id, vcfFileId, projectId, projectIdNumber, chromosome, type, position, endPoints}=obj;

        this._id = id;
        this._vcfFileId = vcfFileId;
        this._projectId = projectId;
        this._projectIdNumber = projectIdNumber;
        this._chromosome = chromosome;
        this._type = type;
        this._endPoints = endPoints;
        this._position = position;
    }

    get id() {
        return this._id;
    }

    get vcfFileId() {
        return this._vcfFileId;
    }

    get projectId() {
        return this._projectId;
    }

    get projectIdNumber() {
        return this._projectIdNumber;
    }

    get chromosome() {
        return this._chromosome;
    }

    get type() {
        return this._type;
    }

    get endPoint() {
        return this._endPoints;
    }

    get position() {
        return this._position;
    }

}

export class PairReadInfo {
    constructor(obj) {
        const {  originalRead, endPoint}=obj;

        this._originalRead = originalRead;
        this._endPoint = endPoint;
    }

    get endPoint() {
        return this._endPoint;
    }



    get originalRead() {
        return this._originalRead;
    }

}

export class EventGeneInfo {
    constructor(obj) {
        const {
            geneId,
            geneName,
            transcriptId,
            startIndex,
            endIndex,
            geneTracks,
            trackId,
            highlight,
            camera,
            mode,
            color,
            pdb,
            chain
        } = obj;

        this._trackId = trackId;
        this.geneTracks = geneTracks;
        this._geneId = geneId;
        this._geneName = geneName || geneId;
        this._transcriptId = transcriptId;
        this._startIndex = startIndex;
        this._endIndex = endIndex;
        this._highlight = highlight;
        this._camera = camera;
        this._mode = mode;
        this._color = color;
        this._pdb = pdb;
        this._chain = chain;
    }

    get geneId() {
        return this._geneId;
    }

    get geneName() {
        return this._geneName;
    }

    get transcriptId() {
        return this._transcriptId;
    }

    set geneTracks(val) {
        this._geneTracks = val;
        this._trackId = (val || [])
            .filter(track => +(track.id) === this.trackId)
            .map(track => track.id)
            .pop()
            || (val || []).map(track => track.id).pop();
    }

    get geneTracks() {
        return this._geneTracks;
    }

    get trackId() {
        return Number.isNaN(Number(this._trackId)) ? undefined : Number(this._trackId);
    }

    get startIndex() {
        return this._startIndex;
    }

    get endIndex() {
        return this._endIndex;
    }

    get highlight() {
        return this._highlight;
    }

    get pdb() {
        return this._pdb;
    }

    get camera() {
        return this._camera;
    }

    get mode() {
        return this._mode;
    }

    get color() {
        return this._color;
    }

    get chain() {
        return this._chain;
    }

}
