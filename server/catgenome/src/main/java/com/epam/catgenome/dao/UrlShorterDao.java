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

package com.epam.catgenome.dao;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcDaoSupport;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

/**
 * Dao helper for work with short urls. It does all work related with saving, loading and updating
 * short url-postfixes.
 * */
public class UrlShorterDao extends NamedParameterJdbcDaoSupport {

    private String insertUrlQuery;
    private String deleteExpiredUrlsQuery;
    private String loadUrlByIdQuery;
    
    public enum Parameters {
        ID, URL, CREATED_DATE, EXPIRED_DATE
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void storeUrl(String id, String url) {
        final MapSqlParameterSource params = new MapSqlParameterSource();

        params.addValue(Parameters.ID.name(), id);
        params.addValue(Parameters.CREATED_DATE.name(), new Date(System.currentTimeMillis()));
        params.addValue(Parameters.URL.name(), url);

        getNamedParameterJdbcTemplate().update(insertUrlQuery, params);
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<String> loadUrlById(String id) {
        final MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue(Parameters.ID.name(), id);

        List<String> query = getNamedParameterJdbcTemplate().query(
            loadUrlByIdQuery,
            params,
            (rs, rowNum) -> rs.getString(Parameters.URL.name())
        );
        return query.isEmpty() ?  Optional.empty() : Optional.of(query.get(0));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void deleteExpiredUrls(Date expiredDate) {
        getJdbcTemplate().update(deleteExpiredUrlsQuery, expiredDate);
    }

    @Required
    public void setLoadUrlByIdQuery(String loadUrlByIdQuery) {
        this.loadUrlByIdQuery = loadUrlByIdQuery;
    }

    @Required
    public void setInsertUrlQuery(String insertUrlQuery) {
        this.insertUrlQuery = insertUrlQuery;
    }
    @Required
    public void setDeleteExpiredUrlsQuery(String deleteExpiredUrlsQuery) {
        this.deleteExpiredUrlsQuery = deleteExpiredUrlsQuery;
    }

}
