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
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
    public NCBITaxonomyVO fetchTaxonomyInfoById(final String id)
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
     * Fetches organisms info from NCBI's taxonomy database
     *
     * @param term search query
     * @return return object with result
     * @throws ExternalDbUnavailableException
     */
    public List<NCBITaxonomyVO> fetchTaxonomyInfosByTerm(final String term)
            throws ExternalDbUnavailableException {
        String ids = StringUtils.join(searchDbForIds("taxonomy", term), ",");
        JsonNode root = summaryEntitiesByIds("taxonomy", ids);
        JsonNode organismNode;
        List<NCBITaxonomyVO> taxonomyVOList = new ArrayList<>();
        for (JsonNode jsonNode : root.path("uids")) {
            int uid = jsonNode.asInt();
            organismNode = root.path(Integer.toString(uid));
            taxonomyVOList.add(new NCBITaxonomyVO(
                    organismNode.path("uid").asText(),
                    organismNode.path("scientificname").asText(),
                    organismNode.path("commonname").asText()));
        }
        return taxonomyVOList;
    }

    /**
     * Search any db by term, returns first id entry
     *
     * @param db   db name
     * @param term term name
     * @return String with data
     * @throws ExternalDbUnavailableException
     */
    public String searchDbForId(final String db, final String term) throws ExternalDbUnavailableException {
        JsonNode idList = getIdsByTerm(db, term);
        String resultId = null;
        if (idList != null && idList.size() > 0) {
            resultId = idList.get(0).asText();
        }
        return resultId;
    }

    /**
     * Search any db by term, returns all entries
     *
     * @param db   db name
     * @param term term name
     * @return List with data
     * @throws ExternalDbUnavailableException
     */
    public List<String> searchDbForIds(final String db, final String term) throws ExternalDbUnavailableException {
        JsonNode idList = getIdsByTerm(db, term);
        List<String> taxIds = new ArrayList<>();
        for (JsonNode id : idList) {
            taxIds.add(id.asText());
        }
        return taxIds;
    }

    private JsonNode getIdsByTerm(final String db, final String term) throws ExternalDbUnavailableException {
        String searchResult = searchById(db, term);
        JsonNode idList;
        try {
            JsonNode root;
            root = mapper.readTree(searchResult).get("esearchresult");
            idList = root.get("idlist");

        } catch (IOException e) {
            throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
        return idList;
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
    public String searchWithHistory(final String db, final String term,
                                    final Integer maxResult) throws ExternalDbUnavailableException {

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
    public JsonNode fetchAnnotationInfoById(final String id) throws ExternalDbUnavailableException {
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
    public String fetchWithHistory(final String queryKey, final String webEnv,
                                   final NCBIDatabase db) throws ExternalDbUnavailableException {
        return fetchData(NCBI_SERVER + NCBI_FETCH, new ParameterNameValue[]{
            new ParameterNameValue(RETMODE_PARAM, "xml"),
            new ParameterNameValue("db", db.name()),
            new ParameterNameValue("query_key", queryKey),
            new ParameterNameValue("WebEnv", webEnv)
        });
    }
}
