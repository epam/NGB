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

package com.epam.catgenome.manager.externaldb.ncbi;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.lang3.StringUtils;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * <p>
 * A class that manages connections to NCBI external database
 * </p>
 */
public class NCBIDataManager extends HttpDataManager {

    private static final Logger LOGGER = Logger.getLogger(NCBIDataManager.class.getName());

    private static final String QUERY_KEY = "query_key";
    private static final String WEB_ENV = "WebEnv";

    protected static final String NCBI_SERVER = "https://eutils.ncbi.nlm.nih.gov/";
    protected static final String NCBI_SUMMARY = "entrez/eutils/esummary.fcgi?";
    protected static final String NCBI_FETCH = "entrez/eutils/efetch.fcgi?";
    protected static final String NCBI_SEARCH = "entrez/eutils/esearch.fcgi?";
    protected static final String NCBI_LINK = "entrez/eutils/elink.fcgi?";
    protected static final String RETMODE_PARAM = "retmode";

    protected JsonMapper mapper = new JsonMapper();

    /**
     * @param id       element id
     * @param dbfrom   dbfrom name
     * @param targetdb targetdb name
     * @param linkname linkname name
     * @return String result from query
     * @throws ExternalDbUnavailableException
     */
    public String link(String id, String dbfrom, String targetdb, String linkname)
            throws ExternalDbUnavailableException {

        List<ParameterNameValue> parametersList = new ArrayList<>();

        parametersList.add(new ParameterNameValue("dbfrom", dbfrom));
        parametersList.add(new ParameterNameValue("db", targetdb));
        parametersList.add(new ParameterNameValue("linkname", linkname));
        parametersList.add(new ParameterNameValue("id", id));
        parametersList.add(new ParameterNameValue("cmd", "neighbor_history"));

        ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + NCBI_LINK, parameterNameValues);
    }

    /**
     * @param db   DB name
     * @param term tern name
     * @return string with data
     * @throws ExternalDbUnavailableException
     */
    public String searchById(String db, String term, String field) throws ExternalDbUnavailableException {

        List<ParameterNameValue> parametersList = new ArrayList<>();

        parametersList.add(new ParameterNameValue("db", db));
        parametersList.add(new ParameterNameValue("term", field != null ? term + "[" + field + "]" : term));
        parametersList.add(new ParameterNameValue(RETMODE_PARAM, "json"));

        ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + NCBI_SEARCH, parameterNameValues);
    }

    public String fetchXmlById(String db, String id, String rettype) throws ExternalDbUnavailableException {
        return fetchById(NCBI_FETCH, db, id, rettype, "xml");
    }

    public String fetchTextById(String db, String id, String rettype) throws ExternalDbUnavailableException {
        return fetchById(NCBI_FETCH, db, id, rettype, "text");
    }

    protected String fetchById(String op, String db, String id, String rettype, String retmode)
            throws ExternalDbUnavailableException {
        LOGGER.info(String.format("Fetching text record by id %s from NCBI db %s", id, db));

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

    public JsonNode summaryEntityById(String db, String id) throws ExternalDbUnavailableException {

        LOGGER.info(String.format("Fetching json record by id %s from NCBI db %s", id, db));

        final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, "json"),
            new ParameterNameValue("db", db),
            new ParameterNameValue("id", id)
        });

        JsonNode root = null;
        try {
            root = mapper.readTree(json).path("result");
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants.
                                                                                  ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
        Iterator<JsonNode> uids = root.path("uids").iterator();

        if (!uids.hasNext()) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants.
                                                                                  ERROR_NO_RESULT_BY_EXTERNAL_DB));
        }

        int uid = uids.next().asInt();
        if (uids.hasNext()) {
            LOGGER.warning("More than 1 uid returned");
        }

        root = root.path(Integer.toString(uid));
        return root;
    }

    public JsonNode summaryWithHistory(String queryKey, String webEnv) throws ExternalDbUnavailableException {

        try {
            final String json = fetchData(NCBI_SERVER + NCBI_SUMMARY, new ParameterNameValue[]{
                new ParameterNameValue(RETMODE_PARAM, "json"),
                new ParameterNameValue(QUERY_KEY, queryKey),
                new ParameterNameValue(WEB_ENV, webEnv)
            });
            return mapper.readTree(json);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants.
                                                                                  ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    public String fetchWithHistory(String queryKey, String webEnv, NCBIDatabase db)
            throws ExternalDbUnavailableException {

        return fetchData(NCBI_SERVER + NCBI_FETCH, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, "xml"),
            new ParameterNameValue("db", db.name()),
            new ParameterNameValue(QUERY_KEY, queryKey),
            new ParameterNameValue(WEB_ENV, webEnv)
        });
    }

    public JsonMapper getMapper() {
        return mapper;
    }

    public void setMapper(JsonMapper mapper) {
        this.mapper = mapper;
    }
}

