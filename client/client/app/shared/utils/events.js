export class EventVariationInfo {
    constructor(obj) {
        const {id, vcfFileId, projectId, chromosome, type, position, endPoints}=obj;

        this._id = id;
        this._vcfFileId = vcfFileId;
        this._projectId = projectId;
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
        const {geneId, transcriptId, startIndex, endIndex, geneTracks, highlight}=obj;

        this._geneTracks = geneTracks;
        this._geneId = geneId;
        this._transcriptId = transcriptId;
        this._startIndex = startIndex;
        this._endIndex = endIndex;
        this._highlight = highlight;
    }

    get geneId() {
        return this._geneId;
    }

    get transcriptId() {
        return this._transcriptId;
    }

    set geneTracks(val) {
        this._geneTracks = val;
    }

    get geneTracks() {
        return this._geneTracks;
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


}