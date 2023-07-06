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

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatMessage;
import com.azure.ai.openai.models.ChatRole;
import com.azure.ai.openai.models.NonAzureOpenAIKeyCredential;
import com.epam.catgenome.entity.llm.LLMProvider;
import com.epam.catgenome.manager.externaldb.PudMedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class LLMService {

    public static final int MAX_PROMT_SIZE = 10000;
    private final String openAIKey;
    private final String promptTemplate;
    private final PudMedService pubmedService;

    public LLMService(final @Value("${llm.openai.api.key:}") String openAIKey,
                      final @Value("${llm.openai.prompt.template:}") String promptTemplate,
                      final PudMedService pubmedService) {
        this.openAIKey = openAIKey;
        this.promptTemplate = promptTemplate;
        this.pubmedService = pubmedService;
    }

    public String getArticleSummary(final List<String> pubMedIDs,
                                    final LLMProvider provider,
                                    final int maxSize,
                                    final double temperature) {
        Assert.isTrue(provider != LLMProvider.GOOGLE_MED_PALM2 && provider != LLMProvider.GOOGLE_PALM_2,
                "Google LLM models are not supported yet.");
        Assert.isTrue(StringUtils.isNotBlank(openAIKey), "OpenAI API key is not configured.");
        final OpenAIClient client = new OpenAIClientBuilder()
                .credential(new NonAzureOpenAIKeyCredential(openAIKey))
                .endpoint("/chat/completions")
                .buildClient();

        final ChatCompletionsOptions options = new ChatCompletionsOptions(
                Collections.singletonList(new ChatMessage(ChatRole.USER).setContent(buildPrompt(pubMedIDs))))
                .setMaxTokens(maxSize)
                .setTemperature(temperature)
                .setN(1);
        final ChatCompletions completions = client.getChatCompletions(provider.getModel(), options);

        log.debug("Model ID={} is created at {}", completions.getId(), completions.getCreated());
        return ListUtils.emptyIfNull(completions.getChoices()).stream().findFirst()
                .map(c -> c.getMessage().getContent())
                .orElseThrow(() -> new IllegalArgumentException("Failed to receive result from LLM"));
    }

    private String buildPrompt(final List<String> pubMedIDs) {
        final String abstracts = pubmedService.getArticleAbstracts(pubMedIDs);
        return promptTemplate + "\n" + StringUtils.left(abstracts, MAX_PROMT_SIZE);
    }
}
