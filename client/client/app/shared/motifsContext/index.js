export default class MotifsContext {
    static instance(dispatcher) {
        return new MotifsContext(dispatcher);
    }

    dispatcher;
    _matches = [];
    _match = null;

    get matches () {
        return this._matches;
    }

    set matches (value) {
        this._matches = value;
    }

    get match () {
        return this._match;
    }

    set match (value) {
        this._match = value;
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this.dispatcher.on('reference:change', () => this.clear(true));
        this.dispatcher.on('chromosome:change', () => this.clear(true));
    }

    setMotifs(currentMatch, allMatches) {
        const changed = JSON.stringify(currentMatch) === JSON.stringify(this.match) ||
            JSON.stringify(allMatches) === JSON.stringify(this.matches);

        if (allMatches && currentMatch) {
            this.matches = allMatches;
            this.match = currentMatch;
        } else {
            this.matches = [];
            this.match = null;
        }
        if (changed) {
            this.dispatcher.emitSimpleEvent('motifs:results:change');
        }
    }

    clear (silent = false) {
        this.matches = [];
        if (!silent) {
            this.dispatcher.emitSimpleEvent('motifs:results:change');
        }
    }
}
