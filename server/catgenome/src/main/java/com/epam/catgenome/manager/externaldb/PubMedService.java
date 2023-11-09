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

package com.epam.catgenome.manager.externaldb;

import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIDatabase;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class PubMedService {

    @Value("${pubmed.search.context:}")
    private String pubMedSearchContext;

    @Value("${pubmed.articles.number.for.abstract:20}")
    private Integer articlesMaxNumber;

    private static final Integer BATCH_SIZE = 500;

    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final NCBIGeneManager ncbiGeneManager;
    private final NCBIDataManager ncbiDataManager;

    @SneakyThrows
    public SearchResult<NCBISummaryVO> fetchPubMedArticles(final PublicationSearchRequest request) {
        final List<GeneId> ncbiGenes = ncbiGeneIdsManager.getNcbiGeneIds(request.getGeneIds());
        final List<String> entrezIds = ncbiGenes.stream()
                .map(g -> g.getEntrezId().toString())
                .collect(Collectors.toList());
        final String retStart = String.valueOf(((request.getPage() - 1) * request.getPageSize()));
        final String retMax = String.valueOf(request.getPageSize());

        final Pair<String, String> historyQuery = ncbiGeneManager.getPubmedHistoryQuery(entrezIds, pubMedSearchContext);
        final List<NCBISummaryVO> articles = ncbiGeneManager.fetchPubmedData(historyQuery, retStart, retMax);
        final int totalCount = ncbiGeneManager.pubmedDataCount(historyQuery);

        final SearchResult<NCBISummaryVO> result = new SearchResult<>();
        result.setItems(articles);
        result.setTotalCount(totalCount);
        return result;
    }

    @SneakyThrows
    public List<NCBISummaryVO> fetchPubMedArticles(final List<String> entrezIds, final long publicationsCount) {
        final Pair<String, String> historyQuery = ncbiGeneManager.getPubmedHistoryQuery(entrezIds, pubMedSearchContext);
        final List<NCBISummaryVO> result = new ArrayList<>();
        final int pages = (int) Math.ceil((double) publicationsCount / BATCH_SIZE);
        for (int i = 0; i < pages; i++) {
            List<NCBISummaryVO> articles = ncbiGeneManager.fetchPubmedData(historyQuery,
                    String.valueOf(i * BATCH_SIZE), BATCH_SIZE.toString());
            result.addAll(articles);
        }
        return result;
    }

    @SneakyThrows
    public int getPublicationsCount(final List<String> entrezIds) {
        final Pair<String, String> historyQuery = ncbiGeneManager.getPubmedHistoryQuery(entrezIds, pubMedSearchContext);
        return ncbiGeneManager.pubmedDataCount(historyQuery);
    }

    @SneakyThrows
    public String getArticleAbstracts(final List<String> pmcIds) {
        final String xml = ncbiDataManager.fetchXmlById(NCBIDatabase.PUBMED.name(),
                String.join(",", pmcIds), "Abstract");
        return parseAbstract(xml);
    }

    public String getArticleAbstracts(final PublicationSearchRequest request) {
        request.setPage(1);
        request.setPageSize(articlesMaxNumber);
        final SearchResult<NCBISummaryVO> result = fetchPubMedArticles(request);
        final List<String> ids = ListUtils.emptyIfNull(result.getItems())
                .stream()
                .map(NCBISummaryVO::getUid)
                .collect(Collectors.toList());
        return getArticleAbstracts(ids);
    }

    private String parseAbstract(final String xml) throws ParserConfigurationException, IOException,
            SAXException, XPathExpressionException {
        final XPath xPath = XPathFactory.newInstance().newXPath();
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = builderFactory.newDocumentBuilder();
        final InputSource is = new InputSource(new StringReader(xml));
        final Document document = builder.parse(is);
        final XPathExpression exp = xPath.compile("//AbstractText");
        final NodeList abstracts = (NodeList)exp.evaluate(document, XPathConstants.NODESET);
        final StringBuilder result = new StringBuilder();
        for (int i = 0; i < abstracts.getLength(); i++) {
            if (result.length() != 0) {
                result.append('\n');
            }
            result.append(abstracts.item(i).getTextContent());
        }
        return result.toString();
    }
}
