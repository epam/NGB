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
import java.util.List;

import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.externaldb.NCBITaxonomyVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ParameterNameValue;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
/**
 * <p>
 * A service class to manage access to NCBI database objects
 * </p>
 */
@Service
public class NCBIAuxiliaryManager extends NCBIDataManager {
    /**
     * Fetches organism info from NCBI's taxonomy database
     * <p>
     * Example query: http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=taxonomy&id=9606&retmode=json
     *
     * @param id taxonomy id
     * @return return object with result
     * @throws ExternalDbUnavailableException
     */
    public NCBITaxonomyVO fetchTaxonomyInfoById(String id)
            throws ExternalDbUnavailableException {
        JsonNode root = summaryEntityById("taxonomy", id);
        try {
            return mapper.treeToValue(root, NCBITaxonomyVO.class);
        } catch (JsonProcessingException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    /**
     * Search any db by term, returns first id entry
     *
     * @param db   db name
     * @param term term name
     * @param field a field to search for. If null, search for all the fields
     * @return String with data
     * @throws ExternalDbUnavailableException
     */

    public String searchDbForId(String db, String term, String field) throws ExternalDbUnavailableException {

        String searchResult = searchById(db, term, field);
        JsonNode idlist;
        try {
            JsonNode root;
            root = mapper.readTree(searchResult).get("esearchresult");
            idlist = root.get("idlist");

        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

        String resultId = null;

        if (idlist != null && idlist.size() > 0) {
            resultId = idlist.get(0).asText();
        }


        return resultId;
    }

    /**
     * Searches given db for given term, stores found ids to server
     *
     * @param db        db name
     * @param term      term name
     * @param maxResult max result size
     * @return string with result
     * @throws ExternalDbUnavailableException
     */
    public String searchWithHistory(String db, String term, Integer maxResult) throws ExternalDbUnavailableException {

        List<ParameterNameValue> parametersList = new ArrayList<>();

        parametersList.add(new ParameterNameValue("db", db));
        parametersList.add(new ParameterNameValue("term", term));
        parametersList.add(new ParameterNameValue("retmax", maxResult + ""));
        parametersList.add(new ParameterNameValue("usehistory", "y"));

        ParameterNameValue[] parameterNameValues = parametersList.toArray(
                new ParameterNameValue[parametersList.size()]);

        return fetchData(NCBI_SERVER + NCBI_SEARCH, parameterNameValues);
    }

    /**
     * Fetch summary info for NCBI annotation database
     * <p>
     * Example query: http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?retmode=xml&db=annotinfo&id=99464
     *
     * @param id build id
     * @return jsonNode from query
     * @throws ExternalDbUnavailableException
     */
    public JsonNode fetchAnnotationInfoById(String id) throws ExternalDbUnavailableException {
        return summaryEntityById("annotinfo", id);
    }

    /**
     * @param queryKey key for query
     * @param webEnv   WebEnv string
     * @param db       from what database
     * @return string with query
     * @throws ExternalDbUnavailableException
     */
    @Override
    public String fetchWithHistory(String queryKey, String webEnv, NCBIDatabase db)
            throws ExternalDbUnavailableException {
        return fetchData(NCBI_SERVER + NCBI_FETCH, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, "xml"),
            new ParameterNameValue("db", db.name()),
            new ParameterNameValue("query_key", queryKey),
            new ParameterNameValue("WebEnv", webEnv)
        });
    }
}
