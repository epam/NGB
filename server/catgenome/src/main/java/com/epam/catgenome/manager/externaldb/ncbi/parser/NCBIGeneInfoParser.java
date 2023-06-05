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

package com.epam.catgenome.manager.externaldb.ncbi.parser;

import static com.epam.catgenome.component.MessageHelper.getMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.externaldb.NCBIGeneVO;
import com.epam.catgenome.controller.vo.externaldb.NCBIShortVarVO;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.ncbi.util.NCBIUtility;

/**
 * <p>
 * Parser for ncbi data
 * </p>
 */
@Service
public class NCBIGeneInfoParser {

    private static final Logger LOG = LoggerFactory.getLogger(NCBIGeneInfoParser.class);
    // eSearch-related xpaths

    private static final String ESEARCH_QUERY_XPATH = "/eSearchResult/QueryKey";
    private static final String ESEARCH_WEBENV_XPATH = "/eSearchResult/WebEnv";

    // eLink-related xpaths

    private static final String ELINK_QUERY_XPATH = "/eLinkResult/LinkSet/LinkSetDbHistory/QueryKey";
    private static final String ELINK_WEBENV_XPATH = "/eLinkResult/LinkSet/WebEnv";

    // Common gene data xpaths

    private static final String GENE_XPATH = "/Entrezgene-Set/Entrezgene";
    private static final String ORGANISM_XPATH =
            GENE_XPATH + "/Entrezgene_source/BioSource/BioSource_org/Org-ref";
    private static final String PRIMARY_SOURCE_PREFIX_XPATH =
            GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_db/" +
                    "Dbtag[1]/Dbtag_db";
    private static final String PRIMARY_SOURCE_XPATH = GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_db/" +
                    "Dbtag[1]/Dbtag_tag/Object-id/Object-id_str";
    private static final String ENTREZ_GENE_TYPE_XPATH = GENE_XPATH + "/Entrezgene_type/@value";
    private static final String ENTREZ_GENE_SUMMARY_XPATH_REL = "Entrezgene_summary";
    private static final String ENTREZ_GENE_SUMMARY_XPATH = GENE_XPATH + "/" + ENTREZ_GENE_SUMMARY_XPATH_REL;
    private static final String REFSEQ_STATUS_XPATH =
            GENE_XPATH + "/Entrezgene_comments/Gene-commentary/Gene-commentary_heading" +
                    "[text()=\"RefSeq Status\"]/../Gene-commentary_label";

    // Interactions xpaths

    private static final String INTERACTIONS_XPATH =
            "//Gene-commentary_heading[text()=\"Interactions\"]/../Gene-commentary_comment/Gene-commentary";

    private static final String REF_ID_XPATH =
            "Gene-commentary_source/Other-source/Other-source_src/" +
                    "Dbtag/Dbtag_tag/Object-id/*[self::Object-id_id or self::Object-id_str]";
    private static final String REF_NAME_XPATH = "Gene-commentary_source/Other-source/Other-source_src/Dbtag/Dbtag_db";

    private static final String OTHSOURCE_ID_XPATH =
            "Other-source_src/Dbtag/Dbtag_tag/Object-id/*[self::Object-id_id or self::Object-id_str]";
    private static final String OTHSOURCE_REF_NAME_XPATH = "Other-source_src/Dbtag/Dbtag_db";
    private static final String OTHSOURCE_ANCHOR_NAME_XPATH = "Other-source_anchor";
    private static final String OFFICIAL_FULL_NAME_XPATH =  GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_desc";
    private static final String OFFICIAL_SYMBOL_XPATH =  GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_locus";
    private static final String LOCUS_TAG_XPATH = GENE_XPATH + "/Entrezgene_gene/Gene-ref/Gene-ref_locus-tag";
    private static final String ALSO_KNOWN_AS_XPATH = GENE_XPATH + "/Entrezgene_gene/Gene-ref/"
            + "Gene-ref_syn/Gene-ref_syn_E";
    private static final String RNA_NAME_XPATH = GENE_XPATH + "/Entrezgene_rna/RNA-ref/RNA-ref_ext/RNA-ref_ext_name";
    private static final String LINEAGE_XPATH = GENE_XPATH + "/Entrezgene_source/BioSource/BioSource_org/Org-ref/"
            + "Org-ref_orgname/OrgName/OrgName_lineage";
    private static final String ID_XPATH = GENE_XPATH + "/Entrezgene_track-info/Gene-track/Gene-track_geneid";
    private static final String PARSING_EXCEPTION_HAPPENED = "Parsing exception happened";
    private static final int LIST_SIZE = 3;
    private static final String DBTAG_DB = "Dbtag_db";
    private static final String OBJECT_ID_STR = "Object-id_str";
    private static final String ENSEMBL = "Ensembl";


    private final XPath xPath = XPathFactory.newInstance().newXPath();

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

            String groupLabel = xPath.compile("/ExchangeSet/Rs/Assembly/@groupLabel").evaluate(document);
            String contigLabel = xPath.compile("/ExchangeSet/Rs/Assembly/Component/@contigLabel").evaluate(document);

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

        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        NCBIGeneVO ncbiGeneVO = new NCBIGeneVO();

        try {

            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            InputSource is = new InputSource(new StringReader(xml));
            Document document = builder.parse(is);
            ncbiGeneVO.setGeneId(xPath.compile(ID_XPATH).evaluate(document));
            ncbiGeneVO.setOrganismScientific(xPath.compile(ORGANISM_XPATH + "/Org-ref_taxname").evaluate(document));
            ncbiGeneVO.setOrganismCommon(xPath.compile(ORGANISM_XPATH + "/Org-ref_common").evaluate(document));
            ncbiGeneVO.setPrimarySource(xPath.compile(PRIMARY_SOURCE_XPATH).evaluate(document));
            ncbiGeneVO.setPrimarySourcePrefix(xPath.compile(PRIMARY_SOURCE_PREFIX_XPATH).evaluate(document));
            ncbiGeneVO.setGeneType(xPath.compile(ENTREZ_GENE_TYPE_XPATH).evaluate(document));
            ncbiGeneVO.setRefSeqStatus(xPath.compile(REFSEQ_STATUS_XPATH).evaluate(document));
            ncbiGeneVO.setGeneSummary(xPath.compile(ENTREZ_GENE_SUMMARY_XPATH).evaluate(document));

            ncbiGeneVO.setOfficialSymbol(xPath.compile(OFFICIAL_SYMBOL_XPATH).evaluate(document));
            ncbiGeneVO.setOfficialFullName(xPath.compile(OFFICIAL_FULL_NAME_XPATH).evaluate(document));
            ncbiGeneVO.setLocusTag(xPath.compile(LOCUS_TAG_XPATH).evaluate(document));
            ncbiGeneVO.setLineage(xPath.compile(LINEAGE_XPATH).evaluate(document));
            ncbiGeneVO.setRnaName(xPath.compile(RNA_NAME_XPATH).evaluate(document));

            NodeList alsoKnown =
                    (NodeList) xPath.compile(ALSO_KNOWN_AS_XPATH).evaluate(document, XPathConstants.NODESET);
            List<String> alsoKnownList = new ArrayList<>(alsoKnown.getLength());
            fillGeneAlsoKnownList(alsoKnown, alsoKnownList);
            ncbiGeneVO.setAlsoKnownAs(alsoKnownList);

            NodeList interactionsNodesList =
                    (NodeList) xPath.compile(INTERACTIONS_XPATH).evaluate(document, XPathConstants.NODESET);

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

    public Map<String, String> parseGeneInfos(final String xml) throws ExternalDbUnavailableException {
        final Map<String, String> descriptions = new HashMap<>();
        try (InputStream is = new ByteArrayInputStream(xml.getBytes())) {
            XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
            xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(is);
            String geneId = "";
            String description = "";
            String dbtagDb = "";
            while (streamReader.hasNext()) {
                streamReader.next();
                if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                    switch (streamReader.getLocalName()) {
                        case DBTAG_DB:
                            dbtagDb = streamReader.getElementText();
                            break;
                        case OBJECT_ID_STR:
                            if (ENSEMBL.equals(dbtagDb)) {
                                geneId = streamReader.getElementText();
                            }
                            break;
                        case ENTREZ_GENE_SUMMARY_XPATH_REL:
                            description = streamReader.getElementText();
                            if (!TextUtils.isBlank(geneId)) {
                                descriptions.put(geneId, description);
                            }
                            break;
                        default:
                            break;
                    }
                }
            }
            streamReader.close();
        } catch (XMLStreamException e) {
            LOG.error(PARSING_EXCEPTION_HAPPENED, e);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
        return descriptions;
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
                pubmedHistoryQuery = xPath.evaluate(ELINK_QUERY_XPATH, xmlDocument);
                pubmedHistoryWebenv = xPath.evaluate(ELINK_WEBENV_XPATH, xmlDocument);
            } else if (NCBIUtility.NCBI_SEARCH == utility) {
                pubmedHistoryQuery = xPath.evaluate(ESEARCH_QUERY_XPATH, xmlDocument);
                pubmedHistoryWebenv = xPath.evaluate(ESEARCH_WEBENV_XPATH, xmlDocument);
            } else {
                pubmedHistoryQuery = StringUtils.EMPTY;
                pubmedHistoryWebenv = StringUtils.EMPTY;
            }

            result = Pair.of(pubmedHistoryQuery, pubmedHistoryWebenv);

        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            LOG.error(PARSING_EXCEPTION_HAPPENED, e);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }

        return result;
    }


    private void parseOtherSource(NodeList refs, List<ReferenceSource> rsList) throws XPathExpressionException {

        for (int i = 0; i < refs.getLength(); i++) {
            Node source = refs.item(i).cloneNode(true);

            String refDbName = xPath.compile(OTHSOURCE_REF_NAME_XPATH).evaluate(source);
            String refId = xPath.compile(OTHSOURCE_ID_XPATH).evaluate(source);
            String anchorName = xPath.compile(OTHSOURCE_ANCHOR_NAME_XPATH).evaluate(source);

            ReferenceSource rs = new ReferenceSource(refDbName, refId, anchorName);
            rsList.add(rs);
        }
    }

    private List<Long> fillList(final NodeList pubmedIdList) throws XPathExpressionException {
        List<Long> result = new ArrayList<>(pubmedIdList.getLength());
        for (int pubmedCnt = 0, length = pubmedIdList.getLength(); pubmedCnt < length; pubmedCnt++) {
            Node pubmedIdNode = pubmedIdList.item(pubmedCnt).cloneNode(true);
            String pubmedIdValue = xPath.compile("Pub_pmid/PubMedId").evaluate(pubmedIdNode);
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

            interactionVO.setDescription(xPath.compile("Gene-commentary_text").evaluate(item));

            NodeList pubmedIdList =
                    (NodeList) xPath.compile("Gene-commentary_refs/Pub").evaluate(item, XPathConstants.NODESET);

            interactionVO.setPubmedIdList(fillList(pubmedIdList));
            interactionVO.setSourceName(xPath.compile(REF_NAME_XPATH).evaluate(item));
            interactionVO.setSourceId(xPath.compile(REF_ID_XPATH).evaluate(item));

            NodeList refs = (NodeList) xPath.
                    compile("Gene-commentary_comment/Gene-commentary").evaluate(item, XPathConstants.NODESET);

            List<ReferenceSource> rsList = new ArrayList<>();

            for (int otherSourcesCnt = 0; otherSourcesCnt < refs.getLength(); otherSourcesCnt++) {
                Node otherGeneItem = refs.item(otherSourcesCnt).cloneNode(true);

                NodeList otherSources = (NodeList) xPath.compile("Gene-commentary_source/Other-source").
                        evaluate(otherGeneItem, XPathConstants.NODESET);

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
