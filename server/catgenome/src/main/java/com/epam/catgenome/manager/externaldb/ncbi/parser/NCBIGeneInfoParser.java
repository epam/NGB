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

package com.epam.catgenome.manager.externaldb.ncbi.parser;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.externaldb.NCBIGeneVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIShortVarVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.exception.NgbException;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIUtility;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.epam.catgenome.component.MessageHelper.getMessage;

/**
 * <p>
 * Parser for ncbi data
 * </p>
 */
@Service
public class NCBIGeneInfoParser {

    private static final Logger LOG = LoggerFactory.getLogger(NCBIGeneInfoParser.class);
    private static final XPath X_PATH = XPathFactory.newInstance().newXPath();
    // eSearch-related xpaths

    private static final String ESEARCH_QUERY_XPATH = "/eSearchResult/QueryKey";
    private static final String ESEARCH_WEBENV_XPATH = "/eSearchResult/WebEnv";

    // eLink-related xpaths

    private static final String ELINK_QUERY_XPATH = "/eLinkResult/LinkSet/LinkSetDbHistory/QueryKey";
    private static final String ELINK_WEBENV_XPATH = "/eLinkResult/LinkSet/WebEnv";

    // Common gene data xpaths

    private static final String GENE_XPATH = "/Entrezgene-Set/Entrezgene";
    private static final String ORGANISM_XPATH = GENE_XPATH + "/Entrezgene_source/BioSource/BioSource_org/Org-ref";

    private static final XPathExpression ORGANISM_SCIENTIFIC_XPATH;
    private static final XPathExpression ORGANISM_COMMON_XPATH;
    private static final XPathExpression PRIMARY_SOURCE_PREFIX_XPATH;
    private static final XPathExpression PRIMARY_SOURCE_XPATH;
    private static final XPathExpression ENTREZ_GENE_TYPE_XPATH;
    private static final XPathExpression ENTREZ_GENE_SUMMARY_XPATH;
    private static final XPathExpression REFSEQ_STATUS_XPATH;
    private static final XPathExpression ENSEMBL_GENE_ID;
    // Interactions xpaths

    private static final XPathExpression INTERACTIONS_XPATH;

    private static final XPathExpression REF_ID_XPATH;
    private static final XPathExpression REF_NAME_XPATH;

    private static final XPathExpression OTHSOURCE_ID_XPATH;
    private static final XPathExpression OTHSOURCE_REF_NAME_XPATH;
    private static final XPathExpression OTHSOURCE_ANCHOR_NAME_XPATH;
    private static final XPathExpression OFFICIAL_FULL_NAME_XPATH;
    private static final XPathExpression OFFICIAL_SYMBOL_XPATH;
    private static final XPathExpression LOCUS_TAG_XPATH;
    private static final XPathExpression ALSO_KNOWN_AS_XPATH;
    private static final XPathExpression RNA_NAME_XPATH;
    private static final XPathExpression LINEAGE_XPATH;
    private static final XPathExpression ID_XPATH;

    private static final XPathExpression PUBMED_ID_XPATH;
    private static final XPathExpression GENE_COMMENTARY_TEXT_XPATH;
    private static final XPathExpression GENE_COMMENTARY_PUB_LIST_XPATH;
    private static final XPathExpression GENE_COMMENTARY_XPATH;
    private static final XPathExpression OTHER_SOURCES_XPATH;

    private static final XPathExpression GROUP_LABEL_XPATH;
    private static final XPathExpression CONTIG_LABEL_XPATH;

    private static final DocumentBuilderFactory DOCUMENT_BUILDER_FACTORY;

    static {
        // disable all validation to speed up processing of large (46k lines XML documents.
        // We expect NCBI responses to be correct anyway
        DOCUMENT_BUILDER_FACTORY = DocumentBuilderFactory.newInstance();
        DOCUMENT_BUILDER_FACTORY.setNamespaceAware(false);
        DOCUMENT_BUILDER_FACTORY.setValidating(false);
        try {
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/namespaces", false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://xml.org/sax/features/validation", false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            DOCUMENT_BUILDER_FACTORY.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (ParserConfigurationException e) {
            LOG.error("Failed to set XML parsing settings", e);
        }

        try {

            ORGANISM_SCIENTIFIC_XPATH = X_PATH.compile(ORGANISM_XPATH + "/Org-ref_taxname");
            ORGANISM_COMMON_XPATH = X_PATH.compile(ORGANISM_XPATH + "/Org-ref_common");
            REFSEQ_STATUS_XPATH = X_PATH.compile(GENE_XPATH +
                    "/Entrezgene_comments/Gene-commentary/Gene-commentary_heading" +
                    "[text()=\"RefSeq Status\"]/../Gene-commentary_label");
            ENTREZ_GENE_SUMMARY_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_summary");
            PRIMARY_SOURCE_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_db/" +
                    "Dbtag[1]/Dbtag_tag/Object-id/Object-id_str");
            ENTREZ_GENE_TYPE_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_type/@value");
            PRIMARY_SOURCE_PREFIX_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_db/" +
                    "Dbtag[1]/Dbtag_db");

            // Interactions xpaths
            INTERACTIONS_XPATH = X_PATH.compile(
                    "//Gene-commentary_heading[text()=\"Interactions\"]/../Gene-commentary_comment/Gene-commentary");
            REF_ID_XPATH = X_PATH.compile("Gene-commentary_source/Other-source/Other-source_src/" +
                    "Dbtag/Dbtag_tag/Object-id/*[self::Object-id_id or self::Object-id_str]");
            REF_NAME_XPATH = X_PATH.compile("Gene-commentary_source/Other-source/Other-source_src/Dbtag/Dbtag_db");

            OTHSOURCE_ID_XPATH = X_PATH.compile(
                    "Other-source_src/Dbtag/Dbtag_tag/Object-id/*[self::Object-id_id or self::Object-id_str]");
            OTHSOURCE_REF_NAME_XPATH = X_PATH.compile("Other-source_src/Dbtag/Dbtag_db");
            OTHSOURCE_ANCHOR_NAME_XPATH = X_PATH.compile("Other-source_anchor");

            OFFICIAL_FULL_NAME_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_desc");
            OFFICIAL_SYMBOL_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_locus");
            LOCUS_TAG_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_locus-tag");
            ALSO_KNOWN_AS_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/"
                    + "Gene-ref_syn/Gene-ref_syn_E");

            RNA_NAME_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_rna/RNA-ref/RNA-ref_ext/RNA-ref_ext_name");
            LINEAGE_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_source/BioSource/BioSource_org/Org-ref/"
                    + "Org-ref_orgname/OrgName/OrgName_lineage");
            ID_XPATH = X_PATH.compile(GENE_XPATH + "/Entrezgene_track-info/Gene-track/Gene-track_geneid");

            PUBMED_ID_XPATH = X_PATH.compile("Pub_pmid/PubMedId");
            GENE_COMMENTARY_TEXT_XPATH = X_PATH.compile("Gene-commentary_text");
            GENE_COMMENTARY_PUB_LIST_XPATH = X_PATH.compile("Gene-commentary_refs/Pub");
            GENE_COMMENTARY_XPATH = X_PATH.compile("Gene-commentary_comment/Gene-commentary");
            OTHER_SOURCES_XPATH = X_PATH.compile("Gene-commentary_source/Other-source");

            GROUP_LABEL_XPATH = X_PATH.compile("/ExchangeSet/Rs/Assembly/@groupLabel");
            CONTIG_LABEL_XPATH = X_PATH.compile("/ExchangeSet/Rs/Assembly/Component/@contigLabel");
            ENSEMBL_GENE_ID = X_PATH.compile(GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_db/" +
                    "Dbtag/Dbtag_db[text()=\"Ensembl\"]/../Dbtag_tag/Object-id/Object-id_str");
        } catch (XPathExpressionException e) {
            throw new NgbException(e);
        }

    }

    private static final String PARSING_EXCEPTION_HAPPENED = "Parsing exception happened";
    private static final int LIST_SIZE = 3;

    /**
     * Method parsing NCBI xml snp information
     *
     * @param xml -- input string with xml content from NCBI db
     */
    public void parseSnpInfo(String xml, NCBIShortVarVO shortVarVO) throws ExternalDbUnavailableException {

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();

        try {

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document document = builder.parse(is);

            String groupLabel = GROUP_LABEL_XPATH.evaluate(document);
            String contigLabel = CONTIG_LABEL_XPATH.evaluate(document);

            shortVarVO.setGenomeLabel(groupLabel);
            shortVarVO.setContigLabel(contigLabel);

        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.error(getMessage(MessagesConstants.ERROR_PARSING, e));

        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
    }

    /**
     * Method parsing NCBI xml gene information
     *
     * @param xml -- input string with xml content from NCBI db
     * @return NCBIGeneVO
     */
    public NCBIGeneVO parseGeneInfo(String xml) throws ExternalDbUnavailableException {
        NCBIGeneVO ncbiGeneVO = new NCBIGeneVO();

        try {
            DocumentBuilder builder = DOCUMENT_BUILDER_FACTORY.newDocumentBuilder();

            InputSource is = new InputSource(new StringReader(xml));
            Document document = builder.parse(is);

            ncbiGeneVO.setGeneId(ID_XPATH.evaluate(document));
            ncbiGeneVO.setOrganismScientific(ORGANISM_SCIENTIFIC_XPATH.evaluate(document));
            ncbiGeneVO.setOrganismCommon(ORGANISM_COMMON_XPATH.evaluate(document));
            ncbiGeneVO.setPrimarySource(PRIMARY_SOURCE_XPATH.evaluate(document));
            ncbiGeneVO.setPrimarySourcePrefix(PRIMARY_SOURCE_PREFIX_XPATH.evaluate(document));
            ncbiGeneVO.setGeneType(ENTREZ_GENE_TYPE_XPATH.evaluate(document));
            ncbiGeneVO.setRefSeqStatus(REFSEQ_STATUS_XPATH.evaluate(document));
            ncbiGeneVO.setGeneSummary(ENTREZ_GENE_SUMMARY_XPATH.evaluate(document));
            ncbiGeneVO.setOfficialSymbol(OFFICIAL_SYMBOL_XPATH.evaluate(document));
            ncbiGeneVO.setOfficialFullName(OFFICIAL_FULL_NAME_XPATH.evaluate(document));
            ncbiGeneVO.setLocusTag(LOCUS_TAG_XPATH.evaluate(document));
            ncbiGeneVO.setLineage(LINEAGE_XPATH.evaluate(document));
            ncbiGeneVO.setRnaName(RNA_NAME_XPATH.evaluate(document));
            ncbiGeneVO.setEnsemblGeneId(ENSEMBL_GENE_ID.evaluate(document));

            NodeList alsoKnown = (NodeList) ALSO_KNOWN_AS_XPATH.evaluate(document, XPathConstants.NODESET);
            List<String> alsoKnownList = new ArrayList<>(alsoKnown.getLength());
            fillGeneAlsoKnownList(alsoKnown, alsoKnownList);
            ncbiGeneVO.setAlsoKnownAs(alsoKnownList);

            NodeList interactionsNodesList = (NodeList) INTERACTIONS_XPATH.evaluate(document, XPathConstants.NODESET);

            List<NCBIGeneVO.NCBIGeneInteractionVO> geneInteractionsList =
                    new ArrayList<>(interactionsNodesList.getLength());

            fillGeneInteractionsList(interactionsNodesList, geneInteractionsList);

            ncbiGeneVO.setInteractions(geneInteractionsList);

        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.error(PARSING_EXCEPTION_HAPPENED, e);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

        return ncbiGeneVO;
    }

    public Pair<String, String> parseHistoryResponse(String srcXml, NCBIUtility utility)
            throws ExternalDbUnavailableException {
        Pair<String, String> result = Pair.of(StringUtils.EMPTY, StringUtils.EMPTY);

        try {

            DocumentBuilderFactory builderFactory =
                    DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(new ByteArrayInputStream(srcXml.getBytes(Charset.defaultCharset())));

            String pubmedHistoryQuery;
            String pubmedHistoryWebenv;

            if (NCBIUtility.NCBI_LINK == utility) {
                pubmedHistoryQuery = X_PATH.evaluate(ELINK_QUERY_XPATH, xmlDocument);
                pubmedHistoryWebenv = X_PATH.evaluate(ELINK_WEBENV_XPATH, xmlDocument);
            } else if (NCBIUtility.NCBI_SEARCH == utility) {
                pubmedHistoryQuery = X_PATH.evaluate(ESEARCH_QUERY_XPATH, xmlDocument);
                pubmedHistoryWebenv = X_PATH.evaluate(ESEARCH_WEBENV_XPATH, xmlDocument);
            } else {
                pubmedHistoryQuery = StringUtils.EMPTY;
                pubmedHistoryWebenv = StringUtils.EMPTY;
            }

            result = Pair.of(pubmedHistoryQuery, pubmedHistoryWebenv);

        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.error(PARSING_EXCEPTION_HAPPENED, e);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants.ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

        return result;
    }


    private void parseOtherSource(NodeList refs, List<ReferenceSource> rsList) throws XPathExpressionException {

        for (int i = 0; i < refs.getLength(); i++) {
            Node source = refs.item(i).cloneNode(true);

            String refDbName = OTHSOURCE_REF_NAME_XPATH.evaluate(source);
            String refId = OTHSOURCE_ID_XPATH.evaluate(source);
            String anchorName = OTHSOURCE_ANCHOR_NAME_XPATH.evaluate(source);

            ReferenceSource rs = new ReferenceSource(refDbName, refId, anchorName);
            rsList.add(rs);
        }
    }

    private List<Long> fillList(final NodeList pubmedIdList) throws XPathExpressionException {
        List<Long> result = new ArrayList<>(pubmedIdList.getLength());
        for (int pubmedCnt = 0, length = pubmedIdList.getLength(); pubmedCnt < length; pubmedCnt++) {
            Node pubmedIdNode = pubmedIdList.item(pubmedCnt).cloneNode(true);
            String pubmedIdValue = PUBMED_ID_XPATH.evaluate(pubmedIdNode);
            if (StringUtils.isNotBlank(pubmedIdValue)) {
                result.add(Long.valueOf(pubmedIdValue));
            }
        }
        return result;
    }

    private void fillGeneAlsoKnownList(NodeList alsoKnown, List<String> alsoKnownList)
            throws XPathExpressionException {
        for (int i = 0; i < alsoKnown.getLength(); i++) {
            Node item = alsoKnown.item(i).cloneNode(true);
            alsoKnownList.add(item.getTextContent());
        }
    }

    private void fillGeneInteractionsList(final NodeList interactionsNodesList,
                                          final List<NCBIGeneVO.NCBIGeneInteractionVO> geneInteractionsList)
            throws XPathExpressionException {
        for (int interactionsCnt = 0; interactionsCnt < interactionsNodesList.getLength(); interactionsCnt++) {

            NCBIGeneVO.NCBIGeneInteractionVO interactionVO = new NCBIGeneVO.NCBIGeneInteractionVO();
            Node item = interactionsNodesList.item(interactionsCnt).cloneNode(true);

            interactionVO.setDescription(GENE_COMMENTARY_TEXT_XPATH.evaluate(item));

            NodeList pubmedIdList =
                    (NodeList) GENE_COMMENTARY_PUB_LIST_XPATH.evaluate(item, XPathConstants.NODESET);

            interactionVO.setPubmedIdList(fillList(pubmedIdList));
            interactionVO.setSourceName(REF_NAME_XPATH.evaluate(item));
            interactionVO.setSourceId(REF_ID_XPATH.evaluate(item));

            NodeList refs = (NodeList) GENE_COMMENTARY_XPATH.evaluate(item, XPathConstants.NODESET);

            List<ReferenceSource> rsList = new ArrayList<>();

            for (int otherSourcesCnt = 0; otherSourcesCnt < refs.getLength(); otherSourcesCnt++) {
                Node otherGeneItem = refs.item(otherSourcesCnt).cloneNode(true);

                NodeList otherSources = (NodeList) OTHER_SOURCES_XPATH.evaluate(otherGeneItem, XPathConstants.NODESET);

                parseOtherSource(otherSources, rsList);
            }

            if (rsList.size() == 3) {

                ReferenceSource product = rsList.get(0);
                interactionVO.setProductRef(product.getName());
                interactionVO.setProductId(product.getId());
                interactionVO.setProductName(product.getAnchor());

                ReferenceSource otherGene = rsList.get(1);
                interactionVO.setOtherGeneRef(otherGene.getName());
                interactionVO.setOtherGeneId(otherGene.getId());
                interactionVO.setOtherGeneName(otherGene.getAnchor());

                ReferenceSource interactant = rsList.get(2);
                interactionVO.setInteractantRef(interactant.getName());
                interactionVO.setInteractantId(interactant.getId());
                interactionVO.setInteractantName(interactant.getAnchor());

            } else if (!rsList.isEmpty() && rsList.size() < LIST_SIZE) {

                ReferenceSource product = rsList.get(0);
                interactionVO.setProductRef(product.getName());
                interactionVO.setProductId(product.getId());
                interactionVO.setProductName(product.getAnchor());
            }

            geneInteractionsList.add(interactionVO);
        }
    }

    public static class ReferenceSource {

        private String name;
        private String id;
        private String anchor;

        public ReferenceSource(String name, String id, String anchor) {
            this.name = name;
            this.id = id;
            this.anchor = anchor;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getAnchor() {
            return anchor;
        }

        public void setAnchor(String anchor) {
            this.anchor = anchor;
        }
    }

}
