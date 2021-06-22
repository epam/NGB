const BLASTResultEvents = {
    changed: 'blast:result:alignment:change'
};

export {BLASTResultEvents};

export {default as parseBtop, BTOPPartType} from './parse-btop';

export default class BLASTContext {
    static instance(dispatcher) {
        return new BLASTContext(dispatcher);
    }

    dispatcher;

    _search;

    _alignments;

    get search () {
        return this._search;
    }

    get alignments () {
        return this._alignments;
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this._alignments = [];
        const clear = this.clear.bind(this);
        this.dispatcher.on('reference:change', () => clear(true));
        this.dispatcher.on('chromosome:change', () => clear(true));
    }

    setAlignments (alignments, search) {
        console.log('set alignments', alignments, search);
        const changed = alignments !== this._alignments || search !== this._search;
        if (alignments && search) {
            this._alignments = alignments;
            this._search = search;
        } else {
            this._alignments = undefined;
            this._search = undefined;
        }
        if (changed) {
            this.dispatcher.emitSimpleEvent(BLASTResultEvents.changed, this.alignments);
        }
    }

    clear (silent = false) {
        this._alignments = [];
        this._search = undefined;
        if (!silent) {
            this.dispatcher.emitSimpleEvent(BLASTResultEvents.changed, []);
        }
    }
}
