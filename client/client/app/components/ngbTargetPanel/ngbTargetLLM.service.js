import {getModelDefaultOptions, modelOptionsEquals} from '../../shared/components/ngbLLM/utilities';

class NgbTargetLLMService {
    static get UID() {
        return 'targetLLMService';
    }

    static instance(dispatcher, utilsDataService) {
        return new NgbTargetLLMService(dispatcher, utilsDataService);
    }

    _models = [];
    _model = undefined;

    constructor(dispatcher, utilsDataService) {
        this.dispatcher = dispatcher;
        this.utilsDataService = utilsDataService;
        this.setModels();
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

    async getLLMSettings() {
        const {llm_settings: llmSettings} = await this.utilsDataService.getDefaultTrackSettings();
        if (!llmSettings) {
            return [];
        }
        return llmSettings.llms || [];
    }

    async setModels() {
        const llmSettings = await this.getLLMSettings();
        if (!llmSettings || !llmSettings.length) {
            this._models = [];
            this._model = undefined;
        } else {
            const providers = llmSettings.map(m => m.provider);
            this._models = providers;
            this._model = getModelDefaultOptions(this._models[0], this._models);
        }
    }
}

export default NgbTargetLLMService;
