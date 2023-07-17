/*
 * MIT License
 *
 * Copyright (c) 2016-2023 EPAM Systems
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.epam.catgenome.manager.llm;

import com.epam.catgenome.entity.llm.LLMProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OpenAIChatGPT35 implements LLMHandler {

    private final String modelName;
    private final int promptSize;
    private final int responseSize;
    private final String promptTemplate;
    private final OpenAIClient openAIClient;

    public OpenAIChatGPT35(final @Value("${llm.openai.chatgpt35.model:gpt-3.5-turbo-16k}") String modelName,
                           final @Value("${llm.openai.chatgpt35.prompt.size:16384}") int promptSize,
                           final @Value("${llm.openai.chatgpt35.response.size:500}") int responseSize,
                           final @Value("${llm.openai.chatgpt35.prompt.template:}") String promptTemplate,
                           final OpenAIClient openAIClient) {
        this.modelName = modelName;
        this.promptSize = promptSize;
        this.responseSize = responseSize;
        this.promptTemplate = promptTemplate;
        this.openAIClient = openAIClient;
    }

    @Override
    public String getSummary(final String text, final double temperature) {
        return openAIClient.getChatCompletion(buildPrompt(promptTemplate, text, promptSize),
                responseSize, modelName, temperature);
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OPENAI_GPT_35;
    }
}
