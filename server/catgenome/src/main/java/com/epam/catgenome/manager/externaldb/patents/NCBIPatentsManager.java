/*
 * MIT License
 *
 * Copyright (c) 2023 EPAM Systems
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

package com.epam.catgenome.manager.externaldb.patents;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.controller.vo.target.PatentsSearchRequest;
import com.epam.catgenome.entity.externaldb.patents.DrugPatent;
import com.epam.catgenome.entity.externaldb.patents.SequencePatent;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIAuxiliaryManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIDataManager;
import com.epam.catgenome.manager.externaldb.pug.NCBIPugManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static com.epam.catgenome.constant.MessagesConstants.ERROR_PARSING;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * <p>
 * A class that manages connections to NCBI external database
 * NOTE: very useful information about possible arguments for different DBs
 * https://www.ncbi.nlm.nih.gov/books/NBK25499/table/chapter4.T._valid_values_of__retmode_and/
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NCBIPatentsManager {
    private static final String PROTEIN_DB = "protein";
    private static final String PUB_CHEM_COMPOUND_DB = "pccompound";
    private static final String PROTEIN_TERM = "Patent[Properties] %s";
    private static final String DRUG_TERM = "has_patent[Filter] %s";
    private static final Pattern DRUG_NAME_PATTERN = Pattern.compile("\\d+\\.\\s");
    private static final String XPATH = "/GBSet/GBSeq";
    private static final String LENGTH = "GBSeq_length";
    private static final String ACCESSION_VERSION = "GBSeq_accession-version";
    private static final String DEFINITION = "GBSeq_definition";
    private static final String ORGANISM = "GBSeq_organism";
    public static final String EMPTY_LINE = "\n\n";
    private final NCBIAuxiliaryManager ncbiAuxiliaryManager;
    private final NCBIDataManager ncbiDataManager;
    private final NCBIPugManager ncbiPugManager;

    public SearchResult<SequencePatent> getProteinPatents(final PatentsSearchRequest request)
            throws ExternalDbUnavailableException {
        final Integer retStart = ((request.getPage() - 1) * request.getPageSize());
        final Integer retMax = request.getPageSize();

        final int count = ncbiAuxiliaryManager.getTotalCount(PROTEIN_DB,
                String.format(PROTEIN_TERM, request.getName()));
        final SearchResult<SequencePatent> result = new SearchResult<>();
        result.setTotalCount(count);
        if (count > 0) {
            final List<String> patentIds = ncbiAuxiliaryManager.searchDbForIds(PROTEIN_DB,
                    String.format(PROTEIN_TERM, request.getName()), retStart, retMax);
            final String xml = ncbiDataManager.fetchXmlById(PROTEIN_DB, join(patentIds, ","), "text");
            final List<SequencePatent> patents = parseProteinPatents(xml);
            result.setItems(patents);
        }
        return result;
    }

    public SearchResult<DrugPatent> getDrugPatents(final PatentsSearchRequest request)
            throws ExternalDbUnavailableException, IOException {
        final Integer retStart = ((request.getPage() - 1) * request.getPageSize());
        final Integer retMax = request.getPageSize();

        final int count = ncbiAuxiliaryManager.getTotalCount(PUB_CHEM_COMPOUND_DB,
                String.format(DRUG_TERM, request.getName()));
        final SearchResult<DrugPatent> result = new SearchResult<>();
        result.setTotalCount(count);
        if (count > 0) {
            final List<String> patentIds = ncbiAuxiliaryManager.searchDbForIds(PUB_CHEM_COMPOUND_DB,
                    String.format(DRUG_TERM, request.getName()), retStart, retMax);
            final String text = ncbiDataManager.fetchTextById(PUB_CHEM_COMPOUND_DB, join(patentIds, ","),
                    "text");
            final List<DrugPatent> patents = parseDrugPatents(text);
            result.setItems(patents);
        }
        return result;
    }

    public List<DrugPatent> getDrugPatents(final String id)
            throws ExternalDbUnavailableException, IOException {
        final List<Long> cids = ncbiPugManager.getCID(id);
        if (CollectionUtils.isEmpty(cids)) {
            return Collections.emptyList();
        }
        final String text = ncbiDataManager.fetchTextById(PUB_CHEM_COMPOUND_DB, join(cids, ","),
                "text");
        final List<DrugPatent> result = parseDrugPatents(text);
        if (CollectionUtils.isEmpty(result)) {
            return Collections.emptyList();
        }
        final Map<Long, List<String>> pugPatents = ncbiPugManager.getPatents(cids);
        result.forEach(p -> {
            List<String> cidPatents = pugPatents.get(Long.parseLong(p.getId()));
            p.setHasPatent(CollectionUtils.isNotEmpty(cidPatents));
        });
        return result;
    }

    private List<SequencePatent> parseProteinPatents(final String xml) throws ExternalDbUnavailableException {
        final DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        final List<SequencePatent> patents = new ArrayList<>();
        final XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            final DocumentBuilder builder = builderFactory.newDocumentBuilder();
            final InputSource is = new InputSource(new StringReader(xml));
            final Document document = builder.parse(is);
            final NodeList sequences = (NodeList) xPath.compile(XPATH).evaluate(document, XPathConstants.NODESET);
            for (int i = 0; i < sequences.getLength(); i++) {
                Node item = sequences.item(i);
                SequencePatent patent = new SequencePatent();
                patent.setLength(Integer.valueOf(xPath.compile(LENGTH).evaluate(item)));
                patent.setId(xPath.compile(ACCESSION_VERSION).evaluate(item));
                patent.setName(xPath.compile(DEFINITION).evaluate(item));
                patent.setOrganism(xPath.compile(ORGANISM).evaluate(item));
                patents.add(patent);
            }
        } catch (ParserConfigurationException | SAXException | XPathExpressionException e) {
            log.error(ERROR_PARSING, e);
        } catch (IOException e) {
            throw new ExternalDbUnavailableException(getMessage(MessagesConstants
                    .ERROR_NO_RESULT_BY_EXTERNAL_DB), e);
        }
        return patents;
    }

    private List<DrugPatent> parseDrugPatents(final String text) throws IOException {
        final List<DrugPatent> patents = new ArrayList<>();
        String[] drugs = text.trim().split(EMPTY_LINE);
        for (String item : drugs) {
            if (!item.contains("Error occurred")) {
                String line;
                try (Reader reader = new StringReader(item);
                     BufferedReader bufferedReader = new BufferedReader(reader)) {
                    DrugPatent patent = new DrugPatent();
                    while ((line = bufferedReader.readLine()) != null) {
                        Matcher m = DRUG_NAME_PATTERN.matcher(line);
                        if (line.startsWith("CID: ")) {
                            patent.setId(line.replace("CID: ", "").trim());
                        } else if (line.startsWith("IUPAC name: ")) {
                            patent.setIupacName(line.replace("IUPAC name: ", "").trim());
                        } else if (line.startsWith("MW: ")) {
                            patent.setMolecularFormula(line.split("MF: ")[1].trim());
                        } else if (m.find()) {
                            patent.setName(m.replaceFirst("").trim());
                        }
                    }
                    patents.add(patent);
                }
            }
        }
        return patents;
    }
}
