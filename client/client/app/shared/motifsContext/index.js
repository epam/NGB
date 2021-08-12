export default class MotifsContext {
    static instance(dispatcher) {
        return new MotifsContext(dispatcher);
    }

    dispatcher;
    _match = {};

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

    setMotif(currentMatch) {
        const changed = JSON.stringify(currentMatch) === JSON.stringify(this.match);

        if (currentMatch) {
            this.match = currentMatch;
        } else {
            this.match = {};
        }
        if (changed) {
            this.dispatcher.emitSimpleEvent('motifs:results:change');
        }
    }

    clear (silent = false) {
        this.match = {};
        if (!silent) {
            this.dispatcher.emitSimpleEvent('motifs:results:change');
        }
    }
}
