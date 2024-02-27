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

import com.epam.catgenome.entity.llm.CustomLLMMessage;
import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;

@Service
public class CustomLLMHandler implements LLMHandler {

    private CustomLLMApi api;
    private Integer promptSize;
    private String promptTemplate;
    private final String firstMessagePrefix;
    private final String lastMessagePrefix;

    public CustomLLMHandler(final @Value("${llm.custom.url:}") String url,
                            final @Value("${llm.custom.token:}") String token,
                            final @Value("${llm.custom.type:custom}") String type,
                            final @Value("${llm.custom.model.name:}") String modelName,
                            final @Value("${llm.custom.prompt.template:}") String promptTemplate,
                            final @Value("${llm.custom.prompt.size:1000}") int promptSize,
                            final @Value("${llm.custom.response.size:500}") int responseSize,
                            final @Value("${llm.custom.first.message.prefix:}") String firstMessagePrefix,
                            final @Value("${llm.custom.last.message.prefix:}") String lastMessagePrefix) {
        if (StringUtils.isBlank(url)) {
            this.api = null;
        }else if (type.equalsIgnoreCase("openai")) {
            this.api = new CustomOpenAILLMClient(url, token, modelName, responseSize);
        } else {
            this.api = new CustomLLMApiClient(url, token, modelName);
        }
        this.promptSize = promptSize;
        this.promptTemplate = promptTemplate;
        this.firstMessagePrefix = firstMessagePrefix;
        this.lastMessagePrefix = lastMessagePrefix;
    }

    @Override
    public String getSummary(final String text, final double temperature) {
        return getSummary(promptTemplate, text, temperature);
    }

    @Override
    public String getSummary(final String prompt, final String text, final double temperature) {
        Assert.notNull(api, "Custom LLM api is not configured.");
        return api.getSummary(new CustomLLMMessage(buildPrompt(prompt, text, promptSize)), temperature);
    }

    @Override
    public String getChatResponse(final List<LLMMessage> messages, final double temperature) {
        Assert.notNull(api, "Custom LLM api is not configured.");
        return api.getChatResponse(
                adjustFirstMessage(adjustLastMessage(messages, lastMessagePrefix), firstMessagePrefix), temperature);
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.CUSTOM;
    }
}
