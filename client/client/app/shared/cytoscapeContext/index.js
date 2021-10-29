class CytoscapeContext {
    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this._info = undefined;
    }

    get info() {
        return this._info;
    }

    static instance (dispatcher) {
        return new CytoscapeContext(dispatcher);
    }

}

export default CytoscapeContext;
