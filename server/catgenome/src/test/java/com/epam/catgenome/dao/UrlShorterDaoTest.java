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

package com.epam.catgenome.dao;

import java.sql.Date;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.epam.catgenome.manager.UrlShorterManager;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:applicationContext-test.xml"})
public class UrlShorterDaoTest extends AbstractDaoTest {

    private static final Date YESTERDAY = new Date(System.currentTimeMillis() - UrlShorterManager.MILLISECONDS_PER_DAY);
    private static final Date TOMORROW = new Date(System.currentTimeMillis() + UrlShorterManager.MILLISECONDS_PER_DAY);

    private final String url = "http://fakeurl.net/fake/fake";
    private final String postfix = "aaaaaa";

    @Autowired
    private UrlShorterDao urlShorterDao;

    @Override
    public void setup() throws Exception {
        Assert.assertNotNull("UrlShorterDao isn't provided.", urlShorterDao);
        super.setup();
    }

    @Before
    public void setUp() {
        urlShorterDao.storeUrl(postfix, url);
    }

    @Test
    public void storeAndLoadUrlShouldStoreCorrectUrl() throws Exception {
        Optional<String> loadedUrl = urlShorterDao.loadUrlById(postfix);
        Assert.assertTrue(loadedUrl.isPresent());
        Assert.assertEquals(loadedUrl.get(), url);
    }

    @Test
    public void deletionOfExpiredUrlsShouldDeleteOnlyExpiredUrls() throws Exception {
        Optional<String> loadedUrl = urlShorterDao.loadUrlById(postfix);

        Assert.assertTrue(loadedUrl.isPresent());

        urlShorterDao.deleteExpiredUrls(YESTERDAY);
        loadedUrl = urlShorterDao.loadUrlById(postfix);
        Assert.assertTrue(loadedUrl.isPresent());
        Assert.assertEquals(loadedUrl.get(), url);

        urlShorterDao.deleteExpiredUrls(TOMORROW);
        loadedUrl = urlShorterDao.loadUrlById(postfix);
        Assert.assertTrue(!loadedUrl.isPresent());
    }

}