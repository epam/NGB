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

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        this._embedded = false;
    }
}

export default AppearanceContext;
