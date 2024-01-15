const TargetGenomicsResultEvents = {
    changed: 'target:genomics:result:alignment:change'
};

export { TargetGenomicsResultEvents };

const TARGET_STORAGE_NAME = 'targetState';

const TARGET_TAB = {
    TARGETS: 'TARGETS',
    IDENTIFICATIONS: 'IDENTIFICATIONS',
    DISEASES: 'DISEASES'
};

const MODE = {
    TABLE: 'table',
    ADD: 'add',
    EDIT: 'edit'
};

export default class TargetContext {

    get targetStorageName() {
        return TARGET_STORAGE_NAME;
    }
    get targetTab() {
        return TARGET_TAB;
    }
    get mode () {
        return MODE;
    }

    _alignments = [];
    _featureCoords;
    _currentState = {};
    _targetsTablePaginationIsVisible;
    _targetsTableActionsIsVisible;
    _targetsTableResetFilterIsVisible = false;

    _targetsFormActionsIsVisible;

    _targetTableTotalPages = 0;

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
                this.currentState = state;
                this.dispatcher.emit('load:target', state);
            }
        } catch (_) {
            reset();
        }
    }

    get targetTableTotalPages() {
        return this._targetTableTotalPages;
    }
    set targetTableTotalPages(value) {
        this._targetTableTotalPages = value;
    }
    get targetsTablePaginationIsVisible() {
        return this._targetsTablePaginationIsVisible;
    }
    set targetsTablePaginationIsVisible(value) {
        this._targetsTablePaginationIsVisible = value;
    }
    get targetsTableActionsIsVisible() {
        return this._targetsTableActionsIsVisible;
    }
    set targetsTableActionsIsVisible(value) {
        this._targetsTableActionsIsVisible = value;
    }
    get targetsTableResetFilterIsVisible() {
        return this._targetsTableResetFilterIsVisible;
    }
    set targetsTableResetFilterIsVisible(value) {
        this._targetsTableResetFilterIsVisible = value;
    }
    get targetsFormActionsIsVisible() {
        return this._targetsFormActionsIsVisible;
    }
    set targetsFormActionsIsVisible(value) {
        this._targetsFormActionsIsVisible = value;
    }

    static instance(dispatcher) {
        return new TargetContext(dispatcher);
    }

    constructor (dispatcher) {
        this.dispatcher = dispatcher;
        const clear = this.clear.bind(this);
        this.dispatcher.on('reference:change', () => clear(true));
        this.dispatcher.on('chromosome:change', () => clear(true));
        this.dispatcher.on('target:table:results:updated', this.setTargetsTableActionsVisibility.bind(this));
    }

    setCurrentTab(tab, mode) {
        const state = {...this.currentState};
        state.tab = tab;
        this.currentState = state;
        this.setTargetsTableActionsVisibility(mode);
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
        state.translationalGenes = getGeneInfo(scope.translationalGenes || []);
        this.currentState = state;
    }

    setCurrentDisease(disease) {
        const state = {...this.currentState};
        state.diseaseId = disease.id;
        state.diseaseName = disease.name;
        this.currentState = state;
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

    setTargetsTableActionsVisibility(mode, resetFilterVisibility) {
        const isTargetTab = this.currentState.tab === this.targetTab.TARGETS;
        const isTableMode = mode === this.mode.TABLE;

        this.targetsTablePaginationIsVisible = isTargetTab && isTableMode && this.targetTableTotalPages > 1;
        this.targetsTableActionsIsVisible = isTargetTab && isTableMode;

        resetFilterVisibility = (resetFilterVisibility !== undefined || (isTargetTab && isTableMode))
            ? resetFilterVisibility : false;
        if (resetFilterVisibility !== undefined) {
            this.targetsTableResetFilterIsVisible = isTargetTab && isTableMode && resetFilterVisibility;
        }
    }

    setTargetsFormActionsVisibility(mode, model) {
        this.targetsFormActionsIsVisible = (mode === this.mode.EDIT && model.isParasite);
    }
}
