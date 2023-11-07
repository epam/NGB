const TargetGenomicsResultEvents = {
    changed: 'target:genomics:result:alignment:change'
};

export { TargetGenomicsResultEvents };

const TARGET_STORAGE_NAME = 'targetState';

export default class TargetContext {

    get targetStorageName() {
        return TARGET_STORAGE_NAME;
    }

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

    get currentState() {
        return this._currentState;
    }
    set currentState(value) {
        this._currentState = value;
        const loadedState = JSON.parse(localStorage.getItem(this.targetStorageName)) || {};
        localStorage.setItem(this.targetStorageName, JSON.stringify({
            ...loadedState,
            ...this._currentState
        }));
        this.report();
    }

    get routeInfo() {
        if (this.currentState) {
            return JSON.stringify(this.currentState);
        }
        return null;
    }

    set routeInfo(value) {
        const reset = () => {
            this.currentState = undefined;
        }
        try {
            if (value) {
                const state = JSON.parse(value);
                this.initState(state);
                this.dispatcher.emit('load:target', state);
            }
        } catch (_) {
            reset();
        }
    }

    static instance(dispatcher) {
        return new TargetContext(dispatcher);
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        const clear = this.clear.bind(this);
        this.dispatcher.on('reference:change', () => clear(true));
        this.dispatcher.on('chromosome:change', () => clear(true));
        this.initState({});
    }

    initState(loadedState) {
        loadedState = {
            ...JSON.parse(localStorage.getItem(this.targetStorageName)),
            ...loadedState
        };
        this._currentState = loadedState;
    }

    setCurrentIdentification(target, scope) {
        const getGeneInfo = (genes) => {
            return genes.map(g => ({
                geneId: g.geneId,
                geneName: g.geneName,
                taxId: g.taxId,
                speciesName: g.speciesName,
            }));
        };
        const state = {...this.currentState};
        state.targetId = target.id;
        state.targetName = target.name;
        state.genesOfInterest = getGeneInfo(scope.genesOfInterest);
        state.translationalGenes = getGeneInfo(scope.translationalGenes);
        this.currentState = state;
    }

    setCurrentDisease(disease) {
        const state = {...this.currentState};
        state.diseaseId = disease.id;
        state.diseaseName = disease.name;
        this.currentState = state;
    }

    report() {
        this.dispatcher.emitGlobalEvent('route:change');
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
