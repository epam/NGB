import {LLM, LLMProperties} from './models';

const ALL_MODELS = Object.values(LLM);

export function getModelDefaultOptions(model, models = ALL_MODELS) {
    if (!model) {
        return {};
    }
    const modelCorrected = !model || !models.includes(model)
        ? models[0]
        : model;
    const parameters = LLMProperties[modelCorrected] || [];
    return parameters.reduce((r, c) => ({
        ...r,
        [c.property]: c.value
    }), {
        type: modelCorrected
    })
}

export function getModelParametersDescription(modelOptions) {
    if (!modelOptions || typeof modelOptions !== 'object') {
        return '';
    }
    const {
        type,
        ...parameters
    } = modelOptions;
    const info = [];
    const modelParameters = LLMProperties[type] || [];
    Object.entries(parameters).forEach(([key, value]) => {
       if (typeof value !== 'undefined') {
           const parameter = modelParameters.find((o) => o.property === key) || {
               property: key
           };
           info.push(`${parameter.name || parameter.property || key}: ${value}`);
       }
    });
    return info.join(', ');
}

export function modelsHasConfigurationProperties(models = ALL_MODELS) {
    return models.some((model) => (LLMProperties[model] || []).length > 0);
}

export function modelOptionsEquals(options1, options2, compareParameters = true) {
    const {
        type: type1,
        ...properties1
    } = options1 || {};
    const {
        type: type2,
        ...properties2
    } = options2 || {};
    if (type1 !== type2) {
        return false;
    }
    if (!compareParameters) {
        return true;
    }
    const keys1 = Object.keys(properties1).sort();
    const keys2 = Object.keys(properties2).sort();
    for (let i = 0; i < keys1.length; i += 1) {
        if (keys1[i] !== keys2[i]) {
            return false;
        }
        const key = keys1[i];
        if (properties1[key] !== properties2[key]) {
            return false;
        }
    }
    return true;
}
