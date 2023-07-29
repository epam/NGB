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

const LLMProperties = {
    [LLM.openAIGPT35]: [
        // {
        //     property: 'temperature',
        //     name: 'Temperature',
        //     type: 'slider', // by default
        //     value: 0.7,
        //     min: 0,
        //     max: 1,
        // },
        // {
        //     property: 'length',
        //     name: 'Words count',
        //     min: 1,
        //     max: Infinity
        // },
    ],
    [LLM.openAIGPT40]: [],
    [LLM.googlePalm2]: [],
};

export {
    LLM,
    LLMName,
    LLMProperties
};
