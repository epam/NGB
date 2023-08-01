const LLM = {
  openAIGPT35: 'OPENAI_GPT_35',
  openAIGPT40: 'OPENAI_GPT_40',
  googlePalm2: 'GOOGLE_PALM_2',
};

const LLMName = {
  [LLM.openAIGPT35]: 'ChatGPT 3.5',
  [LLM.openAIGPT40]: 'ChatGPT 4.0',
  [LLM.googlePalm2]: 'Google PaLM2',
};

const TEMPERATURE_PROPERTY = {
    property: 'temperature',
    name: 'Temperature',
    type: 'slider',
    value: 0.3,
    min: 0,
    max: 1,
};

const MAX_SIZE_PROPERTY = {
    property: 'maxSize',
    name: 'Max size',
    min: 1,
    max: 1000,
    value: 500
}

const LLMProperties = {
    [LLM.openAIGPT35]: [
        TEMPERATURE_PROPERTY,
        MAX_SIZE_PROPERTY
    ],
    [LLM.openAIGPT40]: [
        TEMPERATURE_PROPERTY,
        MAX_SIZE_PROPERTY
    ],
    [LLM.googlePalm2]: [
        TEMPERATURE_PROPERTY,
        MAX_SIZE_PROPERTY
    ],
};

export {
    LLM,
    LLMName,
    LLMProperties
};
