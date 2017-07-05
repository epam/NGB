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

import com.epam.catgenome.dao.UrlShorterDao;
import com.google.common.hash.Hashing;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.util.Optional;

/**
 * A helper service class, generates, saves and loads short urls
 */
@Service
public class UrlShorterManager {

    public static final int MILLISECONDS_PER_DAY = 24 * 60 * 60 * 1000;

    @Value("#{catgenome['urls.expired.period'] ?: 7}")
    private int expiredPeriodParam;

    @Autowired
    private UrlShorterDao urlShorterDao;

    private UrlValidator validator = new UrlValidator(UrlValidator.ALLOW_LOCAL_URLS);

    /**
     * Generate a postfix id for short url by given the original one.
     * */
    @Transactional(propagation = Propagation.REQUIRED)
    public String generateAndSaveShortUrlPostfix(String url) {

        if (!validator.isValid(url)) {
            throw new IllegalArgumentException("Invalid url format: " + url);
        }

        Date expiredDate = new Date(System.currentTimeMillis() - expiredPeriodParam * MILLISECONDS_PER_DAY);
        urlShorterDao.deleteExpiredUrls(expiredDate);

        final String id = Hashing.murmur3_32()
                .hashString(url, StandardCharsets.UTF_8).toString();
        urlShorterDao.storeUrl(id, url);

        return id;
    }


    /**
     * Load original url by given short url-postfix.
     * */
    @Transactional(propagation = Propagation.REQUIRED)
    public Optional<String> getOriginalUrl(String id) {
        return urlShorterDao.loadUrlById(id);
    }

}
