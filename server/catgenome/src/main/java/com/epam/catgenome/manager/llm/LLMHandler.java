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

import com.epam.catgenome.entity.llm.LLMMessage;
import com.epam.catgenome.entity.llm.LLMProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public interface LLMHandler {

    int TOKEN_TO_CHAR = 4;

    String getSummary(String text, double temperature);
    String getChatResponse(List<LLMMessage> messages, double temperature);

    LLMProvider getProvider();

    default String buildPrompt(final String template, final String text, final int maxLength) {
        return StringUtils.left(template + "\n" + text, maxLength * TOKEN_TO_CHAR);
    }

    default List<LLMMessage> adjustLastMessage(final List<LLMMessage> messages, final String prefix) {
        if (CollectionUtils.isEmpty(messages)) {
            return messages;
        }
        return adjustMessage(messages, messages.size() - 1, prefix);
    }

    default List<LLMMessage> adjustFirstMessage(final List<LLMMessage> messages, final String prefix) {
        if (CollectionUtils.isEmpty(messages)) {
            return messages;
        }
        return adjustMessage(messages, 0, prefix);
    }

    default List<LLMMessage> adjustMessage(final List<LLMMessage> messages, final int index, final String prefix) {
        final LLMMessage message = messages.get(index);
        if (!message.getContent().startsWith(prefix)) {
            message.setContent(prefix + " " + message.getContent());
        }
        return messages;
    }
}
