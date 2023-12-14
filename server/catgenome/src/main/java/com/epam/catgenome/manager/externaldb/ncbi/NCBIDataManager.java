/*
 * MIT License
 *
 * Copyright (c) 2016-2021 EPAM Systems
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

package com.epam.catgenome.manager.externaldb.ncbi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.util.TextUtils;
import org.codehaus.jettison.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;

import static com.epam.catgenome.constant.MessagesConstants.ERROR_NO_RESULT_BY_EXTERNAL_DB;

/**
 * <p>
 * A class that manages connections to NCBI external database
 * NOTE: very useful information about possible arguments for different DBs
 * https://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/
 * </p>
 */
@Slf4j
public class NCBIDataManager extends HttpDataManager {

    private static final String QUERY_KEY = "query_key";
    private static final String WEB_ENV = "WebEnv";
    protected static final String NCBI_SERVER = "https://eutils.ncbi.nlm.nih.gov/";
    protected static final String NCBI_SUMMARY = "entrez/eutils/esummary.fcgi?";
    protected static final String NCBI_FETCH = "entrez/eutils/efetch.fcgi?";
    protected static final String NCBI_SEARCH = "entrez/eutils/esearch.fcgi?";
    protected static final String NCBI_LINK = "entrez/eutils/elink.fcgi?";
    protected static final String RETMODE_PARAM = "retmode";
    protected static final String RET_TYPE_PARAM = "rettype";

    protected static final String JSON = "json";

    // entrez/eutils/esummary can return maximum 500 results. If we need to fetch more, we'll have to do pagination.
    // But perhaps it doesn't make sense to fetch more...
    protected static final String MAX_RESULTS_PARAM = "retmax";
    protected static final String START_PARAM = "retstart";
    private static final int MAX_RESULTS_PARAM_VALUE = 500;
    private static final String API_KEY_PARAM = "api_key";

    @Value("#{catgenome['externaldb.ncbi.max.results'] ?: 100}")
    protected Integer ncbiMaxResultsParamValue;

    @Value("${ncbi.api.key:}")
    private String ncbiApiKey;

    @Value("${ncbi.retry.delay:}")
    private Integer ncbiRetryDelay;

    @Value("${ncbi.max.retries:}")
    private Integer ncbiMaxRetries;

    private Integer ncbiRetriesCount = 0;

    @PostConstruct
    public void init() {
        if (ncbiMaxResultsParamValue > MAX_RESULTS_PARAM_VALUE) {
            throw new IllegalArgumentException(
                    "externaldb.ncbi.max.results configuration parameter values should exceed 500");
        }
    }

    protected ObjectMapper mapper = new JsonMapper()
            // NCBI sometimes include control characters like line endings in response.
            // Enable this feature to allow them.
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);

    /**
     * @param id       element id
     * @param dbfrom   dbfrom name
     * @param targetdb targetdb name
     * @param linkname linkname name
     * @return String result from query
     * @throws ExternalDbUnavailableException
     */
    public String link(final String id, final String dbfrom, final String targetdb, final String linkname)
            throws ExternalDbUnavailableException {
        return link(id, dbfrom, targetdb, linkname, null);
    }

    public String link(final String id, final String dbfrom, final String targetdb,
                       final String linkname, final String term)
            throws ExternalDbUnavailableException {

        final List<ParameterNameValue> parametersList = new ArrayList<>();

        parametersList.add(new ParameterNameValue("dbfrom", dbfrom));
        parametersList.add(new ParameterNameValue("db", targetdb));
        parametersList.add(new ParameterNameValue("linkname", linkname));
        parametersList.add(new ParameterNameValue("id", id));
        parametersList.add(new ParameterNameValue("cmd", "neighbor_history"));
        if (!TextUtils.isBlank(term)) {
            parametersList.add(new ParameterNameValue("term", term));
        }

        final ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + NCBI_LINK, parameterNameValues);
    }

    /**
     * @param db   DB name
     * @param term tern name
     * @return string with data
     * @throws ExternalDbUnavailableException
     */
    public String searchById(final String db, final String term) throws ExternalDbUnavailableException {
        return searchById(db, term, null, null, null);
    }

    public String searchById(final String db, final String term,
                             final Integer retStart, final Integer retMax) throws ExternalDbUnavailableException {
        return searchById(db, term, null, retStart, retMax);
    }

    public String searchById(final String db, final String term, final String retType)
            throws ExternalDbUnavailableException {
        return searchById(db, term, retType, null, null);
    }

    public String searchById(final String db, final String term, final String retType,
                             final Integer retStart, final Integer retMax) throws ExternalDbUnavailableException {

        List<ParameterNameValue> parametersList = new ArrayList<>();

        parametersList.add(new ParameterNameValue("db", db));
        parametersList.add(new ParameterNameValue("term", term));
        parametersList.add(new ParameterNameValue(RETMODE_PARAM, JSON));
        if (retType != null) {
            parametersList.add(new ParameterNameValue(RET_TYPE_PARAM, retType));
        }
        if (retMax != null) {
            parametersList.add(new ParameterNameValue(MAX_RESULTS_PARAM, retMax.toString()));
        }
        if (retStart != null) {
            parametersList.add(new ParameterNameValue(START_PARAM, retStart.toString()));
        }
        ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + NCBI_SEARCH, parameterNameValues);
    }

    public String fetchXmlById(final String db, final String id,
                               final String rettype) throws ExternalDbUnavailableException {
        return fetchById(NCBI_FETCH, db, id, rettype, "xml");
    }

    public String fetchTextById(final String db, final String id,
                                final String rettype) throws ExternalDbUnavailableException {
        return fetchById(NCBI_FETCH, db, id, rettype, "text");
    }

    public String fetchJsonById(final String db, final String id,
                                final String rettype) throws ExternalDbUnavailableException {
        return fetchById(NCBI_FETCH, db, id, rettype, "json");
    }

    private String fetchById(final String op, final String db, final String id, final String rettype,
                             final String retmode) throws ExternalDbUnavailableException {
        log.info(String.format("Fetching text record by id %s from NCBI db %s", id, db));

        List<ParameterNameValue> parametersList = new ArrayList<>();

        if (StringUtils.isNotBlank(rettype)) {
            parametersList.add(new ParameterNameValue("rettype", rettype));
        }

        if (StringUtils.isNotBlank(retmode)) {
            parametersList.add(new ParameterNameValue(RETMODE_PARAM, retmode));
        }

        parametersList.add(new ParameterNameValue("db", db));
        parametersList.add(new ParameterNameValue("id", id));

        ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + op, parameterNameValues);
    }

    public JsonNode summaryEntityById(final String db, final String id) throws ExternalDbUnavailableException {

        log.info(String.format("Fetching json record by id %s from NCBI db %s", id, db));

        final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, JSON),
            new ParameterNameValue("db", db),
            new ParameterNameValue("id", id)
        });

        JsonNode root = null;
        try {
            root = mapper.readTree(json).path("result");
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
        Iterator<JsonNode> uids = root.path("uids").iterator();

        if (!uids.hasNext()) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB));
        }

        int uid = uids.next().asInt();
        if (uids.hasNext()) {
            log.warn("More than 1 uid returned");
        }

        root = root.path(Integer.toString(uid));
        return root;
    }

    public JsonNode summaryEntitiesByIds(final String db, final String id) throws ExternalDbUnavailableException {

        log.info(String.format("Fetching json record by ids %s from NCBI db %s", id, db));

        final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, JSON),
            new ParameterNameValue("db", db),
            new ParameterNameValue("id", id)
        });

        try {
            return mapper.readTree(json).path("result");
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    public JsonNode summaryWithHistory(final String queryKey,
                                       final String webEnv) throws ExternalDbUnavailableException {

        try {
            final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
                new ParameterNameValue(RETMODE_PARAM, JSON),
                new ParameterNameValue(QUERY_KEY, queryKey),
                new ParameterNameValue(WEB_ENV, webEnv),
                new ParameterNameValue(MAX_RESULTS_PARAM, ncbiMaxResultsParamValue.toString())
            });
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    public JsonNode summaryWithHistory(final String queryKey, final String webEnv, final String retStart,
                                       final String retMax) throws ExternalDbUnavailableException {
        try {
            final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
                new ParameterNameValue(RETMODE_PARAM, JSON),
                new ParameterNameValue(QUERY_KEY, queryKey),
                new ParameterNameValue(WEB_ENV, webEnv),
                new ParameterNameValue(MAX_RESULTS_PARAM, retMax),
                new ParameterNameValue(START_PARAM, retStart)
            });
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    public String fetchWithHistory(final String queryKey, final String webEnv, final NCBIDatabase db)
            throws ExternalDbUnavailableException {

        return fetchData(NCBI_SERVER + NCBI_FETCH, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, "xml"),
            new ParameterNameValue("db", db.name()),
            new ParameterNameValue(QUERY_KEY, queryKey),
            new ParameterNameValue(WEB_ENV, webEnv)
        });
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public void setMapper(final JsonMapper mapper) {
        this.mapper = mapper;
    }

    @SneakyThrows
    public String getResultFromURL(String location, ParameterNameValue[] params) {
        if (!TextUtils.isBlank(ncbiApiKey) && ncbiRetriesCount == 0) {
            params = ArrayUtils.add(params, new ParameterNameValue(API_KEY_PARAM, ncbiApiKey));
        }
        try {
            final String result = super.getResultFromURL(location, params);
            ncbiRetriesCount = 0;
            return result;
        } catch (ExternalDbUnavailableException e) {
            if (ncbiRetryDelay != null && ncbiMaxRetries != null && ncbiRetriesCount < ncbiMaxRetries) {
                Thread.sleep(ncbiRetryDelay);
                ncbiRetriesCount++;
                return getResultFromURL(location, params);
            } else {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }
        }
    }

    @SneakyThrows
    public String getResultFromHttp(String location, JSONObject object) {
        if (!TextUtils.isBlank(ncbiApiKey) && ncbiRetriesCount == 0) {
            location = location + String.format("?%s=%s", API_KEY_PARAM, ncbiApiKey);
        }
        try {
            final String result = super.getResultFromHttp(location, object);
            ncbiRetriesCount = 0;
            return result;
        } catch (ExternalDbUnavailableException e) {
            if (ncbiRetryDelay != null && ncbiMaxRetries != null && ncbiRetriesCount < ncbiMaxRetries) {
                Thread.sleep(ncbiRetryDelay);
                ncbiRetriesCount++;
                return getResultFromHttp(location, object);
            } else {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }
        }
    }

    @SneakyThrows
    public String getResultFromURL(String location) {
        final Map<String, String> header = new HashMap<>();
        if (!TextUtils.isBlank(ncbiApiKey) && ncbiRetriesCount == 0) {
            header.put(API_KEY_PARAM, ncbiApiKey);
        }
        try {
            final String result = super.getResultFromURL(location, header);
            ncbiRetriesCount = 0;
            return result;
        } catch (ExternalDbUnavailableException e) {
            if (ncbiRetryDelay != null && ncbiMaxRetries != null && ncbiRetriesCount < ncbiMaxRetries) {
                Thread.sleep(ncbiRetryDelay);
                ncbiRetriesCount++;
                return getResultFromURL(location);
            } else {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }
        }
    }
}
