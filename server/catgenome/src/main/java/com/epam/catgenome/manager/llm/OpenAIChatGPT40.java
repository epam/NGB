package com.epam.catgenome.manager.llm;

import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class OpenAIChatGPT40 implements LLMHandler {

    private final String modelName;
    private final int promptSize;
    private final int responseSize;
    private final String promptTemplate;
    private final OpenAIClient openAIClient;
    private final String lastMessagePrefix;
    private final String firstMessagePrefix;

    public OpenAIChatGPT40(final @Value("${llm.openai.chatgpt40.model:gpt-4}") String modelName,
                           final @Value("${llm.openai.chatgpt40.prompt.size:8192}") int promptSize,
                           final @Value("${llm.openai.chatgpt40.response.size:500}") int responseSize,
                           final @Value("${llm.openai.chatgpt40.prompt.template:}") String promptTemplate,
                           final @Value("${llm.openai.chatgpt35.first.message.prefix:}") String firstMessagePrefix,
                           final @Value("${llm.openai.chatgpt35.last.message.prefix:}") String lastMessagePrefix,
                           final @Value("${llm.openai.api.key:}") String openAIKey) {
        this.modelName = modelName;
        this.promptSize = promptSize;
        this.responseSize = responseSize;
        this.promptTemplate = promptTemplate;
        this.firstMessagePrefix = firstMessagePrefix;
        this.lastMessagePrefix = lastMessagePrefix;
        if (StringUtils.isNotBlank(openAIKey)) {
            this.openAIClient = new OpenAIClient(openAIKey);
        } else {
            this.openAIClient = null;
        }
    }

    @Override
    public String getSummary(String text, double temperature) {
        return getSummary(promptTemplate, text, temperature);
    }

    @Override
    public String getSummary(final String prompt, final String text, final double temperature) {
        Assert.notNull(openAIClient, "OpenAI client is not available");
        return openAIClient.getChatCompletion(buildPrompt(prompt, text, promptSize),
                responseSize, modelName, temperature);
    }

    @Override
    public String getChatResponse(final List<LLMMessage> messages, final double temperature) {
        Assert.notNull(openAIClient, "OpenAI client is not available");
        return openAIClient.getChatMessage(
                adjustFirstMessage(adjustLastMessage(messages, lastMessagePrefix), firstMessagePrefix),
                responseSize, modelName, temperature);
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OPENAI_GPT_40;
    }
}
