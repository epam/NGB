import {LLM} from '../../shared/components/ngbLLM/models';
import {getModelDefaultOptions, modelOptionsEquals} from '../../shared/components/ngbLLM/utilities';

const ALL_MODELS = Object.values(LLM);

class NgbTargetLLMService {
    static get UID() {
        return 'targetLLMService';
    }

    static instance(dispatcher) {
        return new NgbTargetLLMService(dispatcher);
    }

    _models = [];
    _model = undefined;

    constructor(dispatcher) {
        this.dispatcher = dispatcher;
        this._models = ALL_MODELS;
        this._model = getModelDefaultOptions(this._models[0], this._models);
    }

    get models() {
        return this._models || [];
    }

    get model() {
        return this._model;
    }

    set model(model) {
        if (!modelOptionsEquals(model, this._model)) {
            const modelTypeChanged = !modelOptionsEquals(model, this._model, false);
            this._model = model;
            if (modelTypeChanged) {
                this.dispatcher.emit('target:identification:publications:model:changed', model);
            }
        }
    }
}

export default NgbTargetLLMService;
