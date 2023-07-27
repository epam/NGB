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

import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.NonAzureOpenAIKeyCredential;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;

@Slf4j
@Service
public class OpenAIClient {

    private final String openAIKey;

    public OpenAIClient(final @Value("${llm.openai.api.key:}") String openAIKey) {
        this.openAIKey = openAIKey;
    }

    public String getChatCompletion(final String prompt,
                                    final int maxSize,
                                    final String model,
                                    final double temperature) {
        Assert.isTrue(StringUtils.isNotBlank(openAIKey), "OpenAI API key is not configured.");
        final com.azure.ai.openai.OpenAIClient client = new OpenAIClientBuilder()
                .credential(new NonAzureOpenAIKeyCredential(openAIKey))
                .buildClient();

        final ChatCompletionsOptions options = new ChatCompletionsOptions(
                Collections.singletonList(new ChatMessage(ChatRole.USER).setContent(prompt)))
                .setMaxTokens(maxSize)
                .setTemperature(temperature)
                .setN(1);
        final ChatCompletions completions = client.getChatCompletions(model, options);

        log.debug("Model ID={} is created at {}", completions.getId(), completions.getCreated());
        return ListUtils.emptyIfNull(completions.getChoices()).stream().findFirst()
                .map(c -> c.getMessage().getContent())
                .orElseThrow(() -> new IllegalArgumentException("Failed to receive result from LLM"));
    }
}
