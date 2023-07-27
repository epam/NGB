package com.epam.catgenome.manager.llm;

import com.epam.catgenome.entity.llm.LLMProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIChatGPT40 implements LLMHandler {

    private final String modelName;
    private final int promptSize;
    private final int responseSize;
    private final String promptTemplate;
    private final OpenAIClient openAIClient;

    public OpenAIChatGPT40(final @Value("${llm.openai.chatgpt40.model:gpt-4}") String modelName,
                           final @Value("${llm.openai.chatgpt40.prompt.size:8192}") int promptSize,
                           final @Value("${llm.openai.chatgpt40.response.size:500}") int responseSize,
                           final @Value("${llm.openai.chatgpt40.prompt.template:}") String promptTemplate,
                           final OpenAIClient openAIClient) {
        this.modelName = modelName;
        this.promptSize = promptSize;
        this.responseSize = responseSize;
        this.promptTemplate = promptTemplate;
        this.openAIClient = openAIClient;
    }

    @Override
    public String getSummary(String text, double temperature) {
        return openAIClient.getChatCompletion(buildPrompt(promptTemplate, text, promptSize),
                responseSize, modelName, temperature);
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OPENAI_GPT_40;
    }
}
