/*
 * MIT License
 *
 * Copyright (c) 2017 EPAM Systems
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

package com.epam.catgenome.manager;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Optional;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class UrlShorterManagerTest {

    private static final String CORRECT = "http://fake.com/faaaaake";
    private static final String CORRECT2 = "http://fake2.com/faaaaake";
    private static final String INCORRECT = "asdasd'awd'";

    /** From docs/md/user-guide/embedding-url.md - raw JSON in ?tracks=, brackets and quotes and all. */
    private static final String EMBEDDING_TRACKS = "http://localhost:8080/catgenome/"
            + "#/GRCh38/2/29224747/29224816"
            + "?tracks=[{\"b\":\"GRCh38\",\"p\":\"SV_Sample1\",\"h\":20},"
            + "{\"b\":\"sv_sample_1.bam\",\"p\":\"SV_Sample1\",\"h\":424}]";

    /** The same document's ?layout= form, which adds braces to the brackets and quotes. */
    private static final String EMBEDDING_LAYOUT = "http://localhost:8080/catgenome/"
            + "#/GRCh38/2/29224747/29224816"
            + "?embedded=On&toolbar=Off"
            + "&layout={\"0\":{\"t\":\"0\",\"p\":\"1\",\"hasHeaders\":\"0\"},\"g\":[{\"n\":\"1\",\"k\":100}]}"
            + "&tracks=[{\"b\":\"GRCh38\",\"p\":\"SV_Sample1\",\"h\":20}]";

    @Autowired
    UrlShorterManager urlShorterManager;
    private String alias = "alias";

    @Test
    public void generateAndSaveShortUrlPostfixShouldSaveCorrectUrl() throws Exception {
        String shortPrefix = urlShorterManager.generateAndSaveShortUrlPostfix(CORRECT, null);
        Optional<String> loaded = urlShorterManager.getOriginalUrl(shortPrefix);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals(loaded.get(), CORRECT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void generateAndSaveShortUrlPostfixShouldThowExceptionWithWrongUrl() throws Exception {
        urlShorterManager.generateAndSaveShortUrlPostfix(INCORRECT, null);
    }

    /**
     * The URL format documented in docs/md/user-guide/embedding-url.md, which POST /generateShortUrl
     * has to keep accepting: raw Jackson JSON substituted into a query parameter, so unencoded
     * {@code { } " [ ]}.
     *
     * <p>This is a pin, not a wish. commons-validator is held at 1.5.0 for exactly this reason: from
     * 1.7 {@code UrlValidator} tightened the query string to what RFC 3986 actually allows, and
     * {@code { } " | ^ \ < >} and space are rejected unencoded. Bumping it therefore has to fail here,
     * loudly, rather than letting the Share Link button, {@code ngb url --alias} and every
     * hand-written embedding URL start answering "Invalid url format" at runtime. See the
     * commons-validator note in server/catgenome/build.gradle for what fixing it properly involves.
     */
    @Test
    public void generateAndSaveShortUrlPostfixShouldAcceptTheDocumentedEmbeddingUrlFormat() throws Exception {
        for (final String url : new String[]{EMBEDDING_TRACKS, EMBEDDING_LAYOUT}) {
            final String shortPrefix = urlShorterManager.generateAndSaveShortUrlPostfix(url, null);
            Assert.assertEquals(url, Optional.of(url), urlShorterManager.getOriginalUrl(shortPrefix));
        }
    }

    @Test
    public void generateAndSaveShortUrlPostfixShouldSaveAcceptAlias() throws Exception {
        String shortPrefix = urlShorterManager.generateAndSaveShortUrlPostfix(CORRECT, alias);
        Assert.assertEquals(shortPrefix, alias);
        Optional<String> loaded = urlShorterManager.getOriginalUrl(shortPrefix);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals(loaded.get(), CORRECT);
    }

    @Test
    public void generateAndSaveShortUrlPostfixShouldNotSaveAcceptAliasInTheSecondTime() throws Exception {
        urlShorterManager.generateAndSaveShortUrlPostfix(CORRECT, alias);
        String shortPrefix = urlShorterManager.generateAndSaveShortUrlPostfix(CORRECT2,  alias);

        Assert.assertNotEquals(shortPrefix, alias);
        Optional<String> loaded = urlShorterManager.getOriginalUrl(shortPrefix);
        Assert.assertTrue(loaded.isPresent());
        Assert.assertEquals(loaded.get(), CORRECT2);
    }

}