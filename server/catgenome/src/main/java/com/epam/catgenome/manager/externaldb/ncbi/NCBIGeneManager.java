/*
 * MIT License
 *
 * Copyright (c) 2016-2023 EPAM Systems
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
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.TextUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.epam.catgenome.component.MessageHelper;
import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.NCBIGeneVO;
import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.parser.NCBIGeneInfoParser;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import static org.apache.commons.lang3.StringUtils.join;

/**
 * <p>
 * A Manager to get gene data from NCBI
 * </p>
 */
@Service
@Slf4j
public class NCBIGeneManager {

    private static final String RESULT_PATH = "result";
    public static final String OR = " or ";
    private static final int MAX_RESULT = 1000;

    private JsonMapper mapper = new JsonMapper();

    @Autowired
    private NCBIAuxiliaryManager ncbiAuxiliaryManager;

    @Autowired
    private NCBIGeneInfoParser geneInfoParser;

    private static final int NUMBER_OF_PUBLICATIONS = 5;
    private static final String UIDS = "uids";

    private static final String NCBI_PUBMED_URL = "https://www.ncbi.nlm.nih.gov/pubmed/%s/";
    private static final String NCBI_BIOSYSTEM_URL = "https://www.ncbi.nlm.nih.gov/biosystems/"
            + "%s?Sel=geneid:%s#show=genes";
    private static final String NCBI_PUBMED_FULL_URL = "https://www.ncbi.nlm.nih.gov/"
            + "pubmed?LinkName=gene_pubmed&from_uid=%s";
    private static final String NCBI_PUBMED_HOMOLOG_URL = "https://www.ncbi.nlm.nih.gov/pubmed?LinkName="
            + "homologene_pubmed&from_uid=%s";
    private static final String NCBI_GENE_LINK = "https://www.ncbi.nlm.nih.gov/gene/%s";

    public List<NCBISummaryVO> fetchPubmedData(final Pair<String, String> historyQuery,
                                               final String retStart, final String retMax) {
        try {
            final String pubmedHistoryQuery = historyQuery.getLeft();
            final String pubmedHistoryWebenv = historyQuery.getRight();
            JsonNode pubmedEntries = ncbiAuxiliaryManager.summaryWithHistory(pubmedHistoryQuery, pubmedHistoryWebenv,
                    retStart, retMax);
            JsonNode pubmedResultRoot = pubmedEntries.path(RESULT_PATH).path(UIDS);
            return parseJsonFromPubmed(pubmedResultRoot, pubmedEntries);
        } catch (ExternalDbUnavailableException | JsonProcessingException e) {
            log.error(e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    public int pubmedDataCount(final Pair<String, String> historyQuery) {
        try {
            String pubmedHistoryQuery = historyQuery.getLeft();
            String pubmedHistoryWebenv = historyQuery.getRight();
            if (TextUtils.isBlank(pubmedHistoryQuery) || TextUtils.isBlank(pubmedHistoryWebenv)) {
                return 0;
            }
            final String xml = ncbiAuxiliaryManager.searchWithHistory(pubmedHistoryQuery, pubmedHistoryWebenv,
                    NCBIDatabase.PUBMED, "0");

            final XPath xPath = XPathFactory.newInstance().newXPath();
            final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder builder = builderFactory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(xml));
            final Document document = builder.parse(is);
            final String count = xPath.compile("eSearchResult/Count").evaluate(document);
            return Integer.parseInt(count);
        } catch (ExternalDbUnavailableException | ParserConfigurationException | XPathExpressionException |
                 SAXException | IOException e) {
            log.error(e.getMessage(), e);
            return 0;
        }
    }

    public Pair<String, String> getPubmedHistoryQuery(final List<String> geneIds, final String term)
            throws ExternalDbUnavailableException {
        final String pubmedQueryXml = ncbiAuxiliaryManager.link(join(geneIds, ","), NCBIDatabase.GENE.name(),
                        NCBIDatabase.PUBMED.name(), "gene_pubmed", term);
        return geneInfoParser.parseHistoryResponse(pubmedQueryXml, NCBIUtility.NCBI_LINK);
    }

    public Pair<String, String> getPubmedHistoryQuery(final String query, final String term)
            throws ExternalDbUnavailableException {
        final String fullQuery = StringUtils.isNotBlank(term) ? String.format("%s AND %s", query, term) : query;
        final String pubmedQueryXml = ncbiAuxiliaryManager.searchWithHistory(NCBIDatabase.PUBMED.name(),
                fullQuery, MAX_RESULT);
        return geneInfoParser.parseHistoryResponse(pubmedQueryXml, NCBIUtility.NCBI_SEARCH);
    }

    public NCBIGeneVO fetchGeneById(final String id) throws ExternalDbUnavailableException {
        NCBIGeneVO ncbiGeneVO = null;

        if (StringUtils.isNotBlank(id)) {
            String realID = fetchExternalId(id);

            String geneInfoXml = ncbiAuxiliaryManager.fetchXmlById(NCBIDatabase.GENE.name(), realID, null);
            ncbiGeneVO = geneInfoParser.parseGeneInfo(geneInfoXml);
            ncbiGeneVO.setLinkToCitations(String.format(NCBI_PUBMED_FULL_URL, ncbiGeneVO.getGeneId()));
            ncbiGeneVO.setGeneLink(String.format(NCBI_GENE_LINK, ncbiGeneVO.getGeneId()));
            String pubmedQueryXml =
                    ncbiAuxiliaryManager.link(realID,
                            NCBIDatabase.GENE.name(), NCBIDatabase.PUBMED.name(), "gene_pubmed");


            Pair<String, String> stringStringPair =
                    geneInfoParser.parseHistoryResponse(pubmedQueryXml, NCBIUtility.NCBI_LINK);

            String pubmedHistoryQuery = stringStringPair.getLeft();
            String pubmedHistoryWebenv = stringStringPair.getRight();

            JsonNode pubmedEntries = ncbiAuxiliaryManager.summaryWithHistory(pubmedHistoryQuery, pubmedHistoryWebenv);
            JsonNode pubmedResultRoot = pubmedEntries.path(RESULT_PATH).path(UIDS);
            try {
                parseJsonFromPubmed(pubmedResultRoot, pubmedEntries, ncbiGeneVO);
            } catch (JsonProcessingException e) {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                        .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }

            String biosystemsQueryXml =
                    ncbiAuxiliaryManager.link(realID,
                            NCBIDatabase.GENE.name(), NCBIDatabase.BIOSYSTEMS.name(), "gene_biosystems");
            Pair<String, String> biosystemsParams =
                    geneInfoParser.parseHistoryResponse(biosystemsQueryXml, NCBIUtility.NCBI_LINK);

            String biosystemsHistoryQuery = biosystemsParams.getLeft();
            String biosystemsHistoryWebenv = biosystemsParams.getRight();

            JsonNode biosystemsEntries =
                    ncbiAuxiliaryManager.summaryWithHistory(biosystemsHistoryQuery, biosystemsHistoryWebenv);
            JsonNode biosystemsResultRoot = biosystemsEntries.path(RESULT_PATH).path(UIDS);
            try {
                parseJsonFromBio(biosystemsResultRoot, biosystemsEntries, ncbiGeneVO);
            } catch (JsonProcessingException e) {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                        .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }

            String homologsQueryXml =
                    ncbiAuxiliaryManager.link(realID,
                            NCBIDatabase.GENE.name(), NCBIDatabase.HOMOLOGENE.name(), "gene_homologene");
            Pair<String, String> homologsParams =
                    geneInfoParser.parseHistoryResponse(homologsQueryXml, NCBIUtility.NCBI_LINK);
            String homologsQuery = homologsParams.getLeft();
            String homologsWebenv = homologsParams.getRight();

            JsonNode homologEntries =
                    ncbiAuxiliaryManager.summaryWithHistory(homologsQuery, homologsWebenv);
            JsonNode homologsResultRoot = homologEntries.path(RESULT_PATH).path(UIDS);
            try {
                parseJsonFromHomolog(homologsResultRoot, homologEntries, ncbiGeneVO);
            } catch (JsonProcessingException e) {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                        .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
            }
        }

        return ncbiGeneVO;
    }

    public Map<GeneId, String> fetchGeneSummaryByIds(final List<GeneId> geneIds)
            throws ExternalDbUnavailableException {
        final Map<GeneId, String> summary = new HashMap<>();
        final Map<String, GeneId> entrezMap = geneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        final JsonNode root = ncbiAuxiliaryManager.summaryEntitiesByIds(NCBIDatabase.GENE.name(),
                join(entrezMap.keySet(), ","));
        JsonNode node;
        for (JsonNode jsonNode : root.path("uids")) {
            String uid = jsonNode.asText();
            node = root.path(uid);
            summary.put(entrezMap.get(uid), node.path("summary").asText());
        }
        return summary;
    }

    @NotNull
    public Map<String, String> getEntrezMap(final List<String> ensemblIds) throws ExternalDbUnavailableException {
        final Map<String, String> entrezIds = new HashMap<>();
        String entrezId;
        for (String id: ensemblIds) {
            try {
                entrezId = fetchExternalId(id);
                if (StringUtils.isNotBlank(entrezId)) {
                    entrezIds.put(entrezId, id);
                }
            } catch (ExternalDbUnavailableException e) {
                log.error("Failed to fetch gene id for {}", id);
            }
        }
        return entrezIds;
    }

    public String fetchExternalId(String id) throws ExternalDbUnavailableException {
        String externalID = id;
        // if ID contains literals then we consider this external ID and perform search
        if (!id.matches("\\d+")) {
            String ncbiId = ncbiAuxiliaryManager.searchDbForId(NCBIDatabase.GENE.name(), externalID);
            if (StringUtils.isNotBlank(ncbiId)) {
                externalID = ncbiId;
            } else {
                throw new ExternalDbUnavailableException(MessageHelper.getMessage(MessagesConstants
                        .ERROR_NO_RESULT_BY_EXTERNAL_DB));
            }
        }
        return externalID;
    }

    public void setGeneInfoParser(NCBIGeneInfoParser geneInfoParser) {
        this.geneInfoParser = geneInfoParser;
    }

    private void parseJsonFromPubmed(final JsonNode pubmedResultRoot, final JsonNode pubmedEntries,
                                     final NCBIGeneVO ncbiGeneVO) throws JsonProcessingException {
        if (pubmedResultRoot.isArray()) {
            ncbiGeneVO.setPubNumber(pubmedResultRoot.size());
            for (final JsonNode objNode : pubmedResultRoot) {
                if (ncbiGeneVO.getPubmedReferences().size() >= NUMBER_OF_PUBLICATIONS) {
                    break;
                }
                NCBISummaryVO pubmedReference = parsePubmedEntry(pubmedEntries, objNode);
                ncbiGeneVO.getPubmedReferences().add(pubmedReference);
            }
        }
    }

    private List<NCBISummaryVO> parseJsonFromPubmed(final JsonNode pubmedResultRoot,
                                                    final JsonNode pubmedEntries) throws JsonProcessingException {
        final List<NCBISummaryVO> result = new ArrayList<>();
        for (final JsonNode objNode : pubmedResultRoot) {
            result.add(parsePubmedEntry(pubmedEntries, objNode));
        }
        return result;
    }

    @NotNull
    private NCBISummaryVO parsePubmedEntry(final JsonNode pubmedEntries,
                                           final JsonNode objNode) throws JsonProcessingException {
        JsonNode jsonNode = pubmedEntries.path(RESULT_PATH).get("" + objNode.asText());
        NCBISummaryVO pubmedReference = mapper.treeToValue(jsonNode, NCBISummaryVO.class);
        pubmedReference.setLink(String.format(NCBI_PUBMED_URL, pubmedReference.getUid()));
        if (CollectionUtils.isNotEmpty(pubmedReference.getAuthors())) {
            pubmedReference.setMultipleAuthors(pubmedReference.getAuthors().size() > 1);
            NCBISummaryVO.NCBIAuthor ncbiAuthor = pubmedReference.getAuthors().get(0);
            pubmedReference.setAuthor(ncbiAuthor);
        }
        return pubmedReference;
    }

    private void parseJsonFromBio(final JsonNode biosystemsResultRoot, final JsonNode biosystemsEntries,
                                  final NCBIGeneVO ncbiGeneVO) throws JsonProcessingException {
        if (biosystemsResultRoot.isArray()) {
            ncbiGeneVO.setPathwaysNumber(biosystemsResultRoot.size());
            List<NCBISummaryVO> pathways = new ArrayList<>(biosystemsResultRoot.size());
            for (final JsonNode objNode : biosystemsResultRoot) {
                JsonNode jsonNode = biosystemsEntries.path(RESULT_PATH).get("" + objNode.asText());
                NCBISummaryVO biosystemsReference = mapper.treeToValue(jsonNode, NCBISummaryVO.class);
                biosystemsReference.setLink(String.format(NCBI_BIOSYSTEM_URL, biosystemsReference.getUid(),
                        ncbiGeneVO.getGeneId()));
                pathways.add(biosystemsReference);
            }
            ncbiGeneVO.setBiosystemsReferences(pathways.stream()
                    .sorted(Comparator.comparing(summary -> summary.getBiosystem().getBiosystemname()))
                    .collect(Collectors.toList()));
        }
    }

    private void parseJsonFromHomolog(JsonNode homologsResultRoot, JsonNode homologEntries,
            NCBIGeneVO ncbiGeneVO) throws JsonProcessingException {
        if (homologsResultRoot.isArray() && homologsResultRoot.size() != 0) {
            final JsonNode objNode = homologsResultRoot.get(0);
            JsonNode jsonNode = homologEntries.path(RESULT_PATH).get("" + objNode.asText());
            NCBISummaryVO summary = mapper.treeToValue(jsonNode, NCBISummaryVO.class);
            ncbiGeneVO.setLinkToHomologsCitations(
                    String.format(NCBI_PUBMED_HOMOLOG_URL, summary.getUid()));
        }
    }
}
