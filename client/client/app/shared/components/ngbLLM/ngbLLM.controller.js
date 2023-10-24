import {LLMName} from './models';
import {
    getModelDefaultOptions, mergeUserOptions,
    modelsHasConfigurationProperties
} from './utilities';

const UUIDGenerator = (() => {
    let uuid = 0;
    return (() => {
        uuid += 1;
        return `llm-controller-${uuid}`;
    });
})();

class NgbLLMController {
    static get UID() {
        return 'ngbLLMController';
    }

    constructor(dispatcher, $scope, utilsDataService) {
        this.dispatcher = dispatcher;
        this.$scope = $scope;
        this.utilsDataService = utilsDataService;
        this.uuid = UUIDGenerator();
        this.setModels();
        this.configureCallback = (payload) => {
            const {
                uuid,
                options
            } = payload || {};
            if (uuid) {
                this.modelOptions = options;
                this.reportModelChanged();
            }
        };
        dispatcher.on('llm:model:configuration:done', this.configureCallback);
        $scope.$on('$destroy', () => {
            dispatcher.removeListener('llm:model:configuration:done', this.configureCallback);
        });
    }

    get modelType() {
        if (!this.modelOptions) {
            return undefined;
        }
        return this.modelOptions.type;
    }

    get modelName() {
        return this.getModelName(this.modelType);
    }

    get llmModels() {
        return this.models;
    }

    get hasConfigurableProperties() {
        return modelsHasConfigurationProperties(this.llmModels);
    }

    getModelName(modelType) {
        if (!modelType) {
            return undefined;
        }
        return LLMName[modelType] || modelType;
    }

    selectModel(modelType) {
        if (this.modelType !== modelType) {
            this.modelOptions = mergeUserOptions(
                getModelDefaultOptions(modelType, this.llmModels),
                this.modelOptions,
            );
            this.reportModelChanged();
        }
    }

    reportModelChanged() {
        if (typeof this.onChange === 'function') {
            this.onChange();
        }
    }

    configure() {
        this.dispatcher.emit('llm:model:configure', {
            options: this.modelOptions,
            uuid: this.uuid,
            models: this.models
        });
    }

    async setModels() {
        const llmSettings = await this.getLLMSettings();
        if (!llmSettings || !llmSettings.length) {
            this.models = [];
        } else {
            this.models = llmSettings.map(m => m.provider);
        }
    }

    async getLLMSettings() {
        const {llm_settings: llmSettings} = await this.utilsDataService.getDefaultTrackSettings();
        if (!llmSettings) {
            return [];
        }
        return llmSettings.llms || [];
    }
}

export default NgbLLMController;
