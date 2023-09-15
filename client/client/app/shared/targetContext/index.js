const TargetGenomicsResultEvents = {
    changed: 'target:genomics:result:alignment:change'
};

export { TargetGenomicsResultEvents };

export default class TargetContext {

    _alignments = [];
    _featureCoords;

    get alignments () {
        return this._alignments;
    }
    get featureCoords () {
        return this._featureCoords;
    }
    set featureCoords (value) {
        this._featureCoords = {
            start: value.startIndex,
            end: value.endIndex
        };
    }

    static instance(dispatcher) {
        return new TargetContext(dispatcher);
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        const clear = this.clear.bind(this);
        this.dispatcher.on('reference:change', () => clear(true));
        this.dispatcher.on('chromosome:change', () => clear(true));
    }

    setAlignments (alignments) {
        const changed = alignments !== this._alignments;
        this._alignments = alignments ? alignments : undefined;
        if (changed) {
            this.setFeatureCoords();
            this.dispatcher.emitSimpleEvent(TargetGenomicsResultEvents.changed, this.alignments);
        }
    }

    setFeatureCoords () {
        if (!this._alignments) return;
        const {queryStart, queryEnd, targetStart, targetEnd} = this._alignments;
        if (queryStart !== targetStart) {
            this.featureCoords.start = this.featureCoords.start + (queryStart - targetStart);
        }
        if (queryEnd !== targetEnd) {
            this.featureCoords.end = this.featureCoords.end + (queryEnd - targetEnd);
        }
    }

    clear (silent = false) {
        this._alignments = [];
        if (!silent) {
            this.dispatcher.emitSimpleEvent(TargetGenomicsResultEvents.changed, []);
        }
    }
}
