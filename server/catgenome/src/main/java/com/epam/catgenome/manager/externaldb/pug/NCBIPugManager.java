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

package com.epam.catgenome.manager.externaldb.pug;

import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.HttpDataManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.util.URLEncoder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * <p>
 * A class that manages connections to NCBI PUG service
 * NOTE: very useful information about possible arguments
 * https://pubchem.ncbi.nlm.nih.gov/docs/pug-rest-tutorial
 * </p>
 */
@Service
@Slf4j
public class NCBIPugManager extends HttpDataManager{

    private static final String NCBI_PUG_SERVER = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/";
    private static final String SMILES_BY_NAME_PATTERN = "compound/name/%s/property/CanonicalSMILES/JSON";
    private static final String SMILES_BY_IDS_PATTERN = "compound/cid/%s/property/CanonicalSMILES,Title/JSON";
    private static final String SYNONYMS_BY_IDS_PATTERN = "compound/cid/%s/synonyms/JSON";
    private static final String COMPOUND_BY_SMILES_PATTERN = "compound/smiles/%s/cids/JSON";
    private static final String COMPOUND_BY_INCHI_PATTERN = "compound/inchi/cids/JSON?inchi=%s";
    private static final String PATENTS_PATTERN = "compound/cid/%s/xrefs/PatentID/JSON";
    private static final Integer BATCH_SIZE = 100;


    public String getSmiles(final String name) throws JsonProcessingException {
        final String location = NCBI_PUG_SERVER + String.format(SMILES_BY_NAME_PATTERN, replaceHttpSymbols(name));
        final String result;
        try {
            result = getResultFromURL(location);
        } catch (ExternalDbUnavailableException e) {
            log.error(e.getMessage(), e);
            return null;
        }
        return parseSmiles(result);
    }

    public Map<String, String> getSmiles(final List<String> compoundIds) throws JsonProcessingException {
        final Map<String, String> allSmiles = new HashMap<>();
        final List<List<String>> subSets = Lists.partition(compoundIds, BATCH_SIZE);
        for (List<String> subSet : subSets) {
            final String location = NCBI_PUG_SERVER + String.format(SMILES_BY_IDS_PATTERN, join(subSet, ","));
            final String result;
            try {
                result = getResultFromURL(location);
            } catch (ExternalDbUnavailableException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            Map<String, String> smiles = getSmilesMap(result);
            allSmiles.putAll(smiles);
        }
        return allSmiles;
    }

    public Map<String, List<String>> getSynonyms(final List<String> compoundIds) throws JsonProcessingException {
        final Map<String, List<String>> allSynonyms = new HashMap<>();
        final List<List<String>> subSets = Lists.partition(compoundIds, BATCH_SIZE);
        for (List<String> subSet : subSets) {
            final String location = NCBI_PUG_SERVER + String.format(SYNONYMS_BY_IDS_PATTERN, join(subSet, ","));
            final String result;
            try {
                result = getResultFromURL(location);
            } catch (ExternalDbUnavailableException e) {
                log.error(e.getMessage(), e);
                return null;
            }
            Map<String, List<String>> synonyms = getSynonymsMap(result);
            allSynonyms.putAll(synonyms);
        }
        return allSynonyms;
    }

    public List<Long> getCID(final String id) throws JsonProcessingException {
        String pattern;
        if (StringUtils.isNumeric(id)) {
            return Collections.singletonList(Long.parseLong(id));
        } else if (isInChI(id)) {
            pattern = COMPOUND_BY_INCHI_PATTERN;
        } else if (isSmiles(id)) {
            pattern = COMPOUND_BY_SMILES_PATTERN;
        } else {
            return Collections.emptyList();
        }
        final String location = NCBI_PUG_SERVER + String.format(pattern, replaceHttpSymbols(id));
        final String result;
        try {
            result = getResultFromURL(location);
        } catch (ExternalDbUnavailableException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
        return parseCIDs(result);
    }

    public Map<Long, List<String>> getPatents(final List<Long> cids)
            throws JsonProcessingException {
        final String location = NCBI_PUG_SERVER + String.format(PATENTS_PATTERN, join(cids, ","));
        final String result;
        try {
            result = getResultFromURL(location);
        } catch (ExternalDbUnavailableException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyMap();
        }
        return parsePatents(result);
    }

    private String parseSmiles(final String data) throws JsonProcessingException {
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node = objectMapper.readTree(data);
        final JsonNode properties = node.at("/PropertyTable/Properties");
        if (properties.isArray()) {
            Iterator<JsonNode> elements = properties.elements();
            if (elements.hasNext()) {
                JsonNode property = elements.next();
                return property.at("/CanonicalSMILES").asText();
            }
        }
        return null;
    }

    private Map<String, String> getSmilesMap(final String data) throws JsonProcessingException {
        final Map<String, String> result = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node = objectMapper.readTree(data);
        final JsonNode properties = node.at("/PropertyTable/Properties");
        if (properties.isArray()) {
            Iterator<JsonNode> elements = properties.elements();
            while (elements.hasNext()) {
                JsonNode property = elements.next();
                result.put(property.at("/CID").asText(), property.at("/CanonicalSMILES").asText());
            }
        }
        return result;
    }

    private Map<String, List<String>> getSynonymsMap(final String data) throws JsonProcessingException {
        final Map<String, List<String>> result = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node = objectMapper.readTree(data);
        final JsonNode properties = node.at("/InformationList/Information");
        if (properties.isArray()) {
            Iterator<JsonNode> elements = properties.elements();
            while (elements.hasNext()) {
                JsonNode property = elements.next();
                JsonNode synonymsNode = property.at("/Synonym");
                List<String> synonyms = new ArrayList<>();
                if (synonymsNode.isArray()) {
                    Iterator<JsonNode> synonymsElements = synonymsNode.elements();
                    while (synonymsElements.hasNext()) {
                        synonyms.add(synonymsElements.next().asText());
                    }
                }
                result.put(property.at("/CID").asText(), synonyms);
            }
        }
        return result;
    }

    private List<Long> parseCIDs(final String data) throws JsonProcessingException {
        final List<Long> result = new ArrayList<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node = objectMapper.readTree(data);
        final JsonNode properties = node.at("/IdentifierList/CID");
        if (properties.isArray()) {
            Iterator<JsonNode> elements = properties.elements();
            while (elements.hasNext()) {
                long cid = elements.next().asLong();
                if (cid != 0) {
                    result.add(cid);
                }
            }
        }
        return result;
    }

    private Map<Long, List<String>> parsePatents(final String data) throws JsonProcessingException {
        final Map<Long, List<String>> result = new HashMap<>();
        final ObjectMapper objectMapper = new ObjectMapper();
        final JsonNode node = objectMapper.readTree(data);
        final JsonNode information = node.at("/InformationList/Information");
        if (information.isArray()) {
            Iterator<JsonNode> elements = information.elements();
            while (elements.hasNext()) {
                List<String> patents = new ArrayList<>();
                JsonNode element = elements.next();
                JsonNode patentIdsNode = element.at("/PatentID");
                Long cid = element.at("/CID").asLong();
                if (patentIdsNode.isArray()) {
                    Iterator<JsonNode> patentIds = patentIdsNode.elements();
                    while (patentIds.hasNext()){
                        patents.add(patentIds.next().asText());
                    }
                    result.put(cid, patents);
                }
            }
        }
        return result;
    }

    private static boolean isSmiles(final String data) {
        final Pattern smilesPattern = Pattern.compile("[A-Za-z0-9@\\+\\-\\.\\(\\)\\[\\]/\\\\=#$]+");
        return smilesPattern.matcher(data).matches();
    }

    private static boolean isInChI(final String data) {
        return data.startsWith("InChI=");
    }

    private static String replaceHttpSymbols(final String data) {
        return URLEncoder.DEFAULT.encode(data, StandardCharsets.UTF_8.toString());
    }
}
