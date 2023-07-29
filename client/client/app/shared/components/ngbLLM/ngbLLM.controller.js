import {LLM, LLMName} from './models';
import {
    getModelDefaultOptions,
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

    constructor(dispatcher, $scope) {
        this.dispatcher = dispatcher;
        this.$scope = $scope;
        this.uuid = UUIDGenerator();
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
        return this.models && Array.isArray(this.models) && this.models.length > 0
            ? this.models
            : Object.values(LLM);
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
            this.modelOptions = getModelDefaultOptions(modelType);
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
            uuid: this.uuid
        });
    }
}

export default NgbLLMController;
