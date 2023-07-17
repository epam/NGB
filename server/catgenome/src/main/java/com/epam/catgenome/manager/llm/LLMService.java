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
import com.epam.catgenome.manager.externaldb.PudMedService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LLMService {

    private final PudMedService pubmedService;
    private final Map<LLMProvider, LLMHandler> handlers;

    public LLMService(final List<LLMHandler> handlers,
                      final PudMedService pubmedService) {
        this.pubmedService = pubmedService;
        this.handlers = ListUtils.emptyIfNull(handlers).stream()
                .collect(Collectors.toMap(LLMHandler::getProvider, Function.identity()));
    }

    public String getArticleSummary(final List<String> pubMedIDs,
                                    final LLMProvider provider,
                                    final int maxSize,
                                    final double temperature) {
        final LLMHandler handler = getHandler(provider);
        return handler.getSummary(pubmedService.getArticleAbstracts(pubMedIDs), temperature);
    }

    private LLMHandler getHandler(final LLMProvider provider) {
        final LLMHandler handler = handlers.get(provider);
        Assert.notNull(handler, provider + " is not supported.");
        return handler;
    }
}
