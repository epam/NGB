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

    _searchResults;

    _alignment;

    get searchResults () {
        return this._searchResults;
    }

    get alignment () {
        return this._alignment;
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        const clear = this.clear.bind(this);
        this.dispatcher.on('reference:change', () => clear(true));
        this.dispatcher.on('chromosome:change', () => clear(true));
    }

    setAlignment (alignment, searchResults) {
        const changed = alignment !== this._alignment || searchResults !== this._searchResults;
        if (alignment && searchResults) {
            this._alignment = alignment;
            this._searchResults = searchResults;
        } else {
            this._alignment = undefined;
            this._searchResults = undefined;
        }
        if (changed) {
            this.dispatcher.emitSimpleEvent(BLASTResultEvents.changed, this.alignment);
        }
    }

    clear (silent = false) {
        this._alignment = undefined;
        this._searchResults = undefined;
        if (!silent) {
            this.dispatcher.emitSimpleEvent(BLASTResultEvents.changed, undefined);
        }
    }
}
