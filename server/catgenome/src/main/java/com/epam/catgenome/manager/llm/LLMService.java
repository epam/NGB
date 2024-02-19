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

import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.entity.externaldb.patents.google.GooglePatent;
import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import com.epam.catgenome.manager.externaldb.PubMedService;
import com.epam.catgenome.manager.externaldb.patents.GooglePatentManager;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LLMService {

    private final PubMedService pubmedService;
    private final GooglePatentManager patentManager;
    private final Map<LLMProvider, LLMHandler> handlers;
    private final String patentPrompt;

    public LLMService(final List<LLMHandler> handlers,
                      final GooglePatentManager patentManager,
                      final PubMedService pubmedService,
                      final @Value("${llm.patent.prompt:}") String patentPrompt) {
        this.pubmedService = pubmedService;
        this.patentManager = patentManager;
        this.patentPrompt = patentPrompt;
        this.handlers = ListUtils.emptyIfNull(handlers).stream()
                .collect(Collectors.toMap(LLMHandler::getProvider, Function.identity()));
    }

    public String getArticleSummary(final List<String> pubMedIDs,
                                    final LLMProvider provider,
                                    final int maxSize,
                                    final double temperature) {
        return getHandler(provider)
                .getSummary(pubmedService.getArticleAbstracts(pubMedIDs), temperature);
    }

    public String getPatentSummary(final String query,
                                   final LLMProvider provider,
                                   final int maxSize,
                                   final double temperature) {
        return getHandler(provider)
                .getSummary(patentPrompt, builtPatentText(patentManager.getAllPatents(query)), temperature);
    }


    public String getGeneArticleSummary(final List<String> geneIds,
                                        final LLMProvider provider,
                                        final int maxSize,
                                        final double temperature) {
        final PublicationSearchRequest request = new PublicationSearchRequest();
        request.setGeneIds(geneIds);
        return getHandler(provider).getSummary(pubmedService.getArticleAbstracts(request), temperature);
    }

    public String getChatResponse(final List<LLMMessage> messages,
                                  final LLMProvider provider,
                                  final int maxSize,
                                  final double temperature) {
        return getHandler(provider).getChatResponse(messages, temperature);
    }


    private LLMHandler getHandler(final LLMProvider provider) {
        final LLMHandler handler = handlers.get(provider);
        Assert.notNull(handler, provider + " is not supported.");
        return handler;
    }

    private String builtPatentText(final List<GooglePatent> patents) {
        return ListUtils.emptyIfNull(patents)
                .stream()
                .map(patent -> {
                    final StringBuilder result = new StringBuilder();
                    final String description = StringUtils.defaultString(patent.getDescription(), patent.getSnippet());
                    result.append("Patent: ")
                            .append(patent.getFullTitle())
                            .append(' ')
                            .append(description)
                            .append(" Link: ")
                            .append(patent.getLink());
                    return result.toString();
                }).collect(Collectors.joining("\n"));
    }

}
