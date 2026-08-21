/*
 * MIT License
 *
 * Copyright (c) 2016 EPAM Systems
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

package com.epam.catgenome.app;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

import com.epam.catgenome.exception.LuceneIndexVersionException;

/**
 * Makes {@link LuceneIndexVersionCheck}'s refusal to start print as the upgrade instruction it is,
 * instead of as a {@code BeanCreationException} stack trace.
 *
 * <p>Registered in {@code META-INF/spring.factories}, which is how Spring Boot loads failure
 * analyzers - they are looked up before the application context exists, so they cannot be beans.
 * When one returns an analysis, {@code SpringApplication} logs that analysis at ERROR and does not
 * log the stack trace at all (it stays available at DEBUG).
 *
 * <p>The whole message is the description: it is written to be read on its own, because the same
 * text has to work when the exception surfaces from a request rather than from startup.
 */
public class LuceneIndexVersionFailureAnalyzer extends AbstractFailureAnalyzer<LuceneIndexVersionException> {

    @Override
    protected FailureAnalysis analyze(final Throwable rootFailure, final LuceneIndexVersionException cause) {
        return new FailureAnalysis(cause.getMessage(), null, cause);
    }
}
