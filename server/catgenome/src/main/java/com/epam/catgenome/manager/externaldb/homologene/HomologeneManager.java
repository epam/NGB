/*
 * MIT License
 *
 * Copyright (c) 2021 EPAM Systems
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
package com.epam.catgenome.manager.externaldb.homologene;

import com.epam.catgenome.component.MessageCode;
import com.epam.catgenome.entity.externaldb.homologene.Domain;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.util.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.epam.catgenome.component.MessageHelper.getMessage;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
public class HomologeneManager {

    private static final String TERM_SPLIT_TOKEN = " ";
    private static final String GENE_FIELDS_LINE_DELIMITER = "|";

    @Value("${homologene.index.directory}")
    private String indexDirectory;

    public HomologeneSearchResult<HomologeneEntry> searchHomologenes(final HomologeneSearchRequest query)
            throws IOException {
        final List<HomologeneEntry> entries = new ArrayList<>();
        final HomologeneSearchResult<HomologeneEntry> searchResult = new HomologeneSearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final Integer page = query.getPage();
            Assert.isTrue(page > 0, "Page number should be > 0");

            final Integer pageSize = query.getPageSize();
            Assert.isTrue(pageSize > 0, "Page size should be > 0");
            final int hits = page * pageSize;

            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(buildSearchQuery(query.getQuery()), hits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);

            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                entries.add(
                    HomologeneEntry.builder()
                        .groupId(getGroupId(doc))
                        .taxId(getTaxId(doc))
                        .version(getVersion(doc))
                        .caption(getCaption(doc))
                        .genes(getGenes(doc))
                        .build()
                );
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public void importHomologeneDatabase(final String databasePath) throws IOException, ParseException {
        File file = new File(databasePath);
        Assert.isTrue(file.isFile() && file.canRead(), getMessage(MessageCode.RESOURCE_NOT_FOUND));
        double time1 = Utils.getSystemTimeMilliseconds();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (HomologeneEntry entry: readHomologenes(databasePath)) {
                addDoc(writer, entry);
            }
        }
        double time2 = Utils.getSystemTimeMilliseconds();
        log.debug("Homologene File import took {} ms", time2 - time1);
    }

    private Query buildSearchQuery(final String terms) {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        for (String term: terms.split(TERM_SPLIT_TOKEN)) {
            mainBuilder.add(
                new BooleanQuery.Builder()
                    .add(buildPrefixQuery(term, IndexFields.QUERY_FIELDS), BooleanClause.Occur.SHOULD)
                    .build(),
                BooleanClause.Occur.MUST
            );
        }
        return mainBuilder.build();
    }

    private PrefixQuery buildPrefixQuery(final String term, final IndexFields field) {
        return new PrefixQuery(new Term(field.getFieldName(), term.toLowerCase()));
    }

    private long getGroupId(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.GROUP_ID.getFieldName()).stringValue());
    }

    private long getVersion(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.VERSION.getFieldName()).stringValue());
    }

    private String getCaption(final Document doc) {
        return doc.getField(IndexFields.CAPTION.getFieldName()).stringValue();
    }

    private long getTaxId(final Document doc) {
        return Long.parseLong(doc.getField(IndexFields.TAX_ID.getFieldName()).stringValue());
    }

    private List<Gene> getGenes(final Document doc) {
        return doc.getField(IndexFields.GENES.getFieldName()) == null ? null
                : deserializeGenes(doc.getField(IndexFields.GENES.getFieldName()).stringValue());
    }

    @Getter
    private enum IndexFields {
        GROUP_ID("groupId"),
        VERSION("version"),
        CAPTION("caption"),
        TAX_ID("taxId"),
        QUERY_FIELDS("queryFields"),
        GENES("genes");

        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    @SneakyThrows
    public List<HomologeneEntry> readHomologenes(final String path) {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        InputStream inputStream = new FileInputStream(path);
        XMLStreamReader streamReader = xmlInputFactory.createXMLStreamReader(inputStream);
        List<HomologeneEntry> homologeneEntries = new ArrayList<>();
        List<Gene> genes = new ArrayList<>();
        List<String> aliases = new ArrayList<>();
        List<Domain> domains = new ArrayList<>();
        HomologeneEntry homologeneEntry = null;
        Gene gene = null;
        Domain domain = null;
        while(streamReader.hasNext()) {
            streamReader.next();
            if (streamReader.getEventType() == XMLStreamReader.START_ELEMENT) {
                switch (streamReader.getLocalName()) {
                    case "HG-Entry":
                        homologeneEntry = new HomologeneEntry();
                        break;
                    case "HG-Entry_hg-id":
                        homologeneEntry.setGroupId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Entry_version":
                        homologeneEntry.setVersion(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Entry_caption":
                        homologeneEntry.setCaption(streamReader.getElementText());
                        break;
                    case "HG-Entry_taxid":
                        homologeneEntry.setTaxId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene":
                        gene = new Gene();
                        break;
                    case "HG-Gene_geneid":
                        gene.setGeneId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_symbol":
                        gene.setSymbol(streamReader.getElementText());
                        break;
                    case "HG-Gene_title":
                        gene.setTitle(streamReader.getElementText());
                        break;
                    case "HG-Gene_taxid":
                        gene.setTaxId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_prot-gi":
                        gene.setProtGi(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_prot-acc":
                        gene.setProtAcc(streamReader.getElementText());
                        break;
                    case "HG-Gene_prot-len":
                        gene.setProtLen(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_nuc-gi":
                        gene.setNucGi(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Gene_nuc-acc":
                        gene.setNucAcc(streamReader.getElementText());
                        break;
                    case "HG-Gene_locus-tag":
                        gene.setLocusTag(streamReader.getElementText());
                        break;
                    case "HG-Gene_aliases_E":
                        aliases.add(streamReader.getElementText());
                        break;
                    case "HG-Domain":
                        domain = new Domain();
                        break;
                    case "HG-Domain_begin":
                        domain.setBegin(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_end":
                        domain.setEnd(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_pssm-id":
                        domain.setPssmId(Long.valueOf(streamReader.getElementText()));
                        break;
                    case "HG-Domain_cdd-id":
                        domain.setCddId(streamReader.getElementText());
                        break;
                    case "HG-Domain_cdd-name":
                        domain.setCddName(streamReader.getElementText());
                        break;
                    default:
                        break;
                }
            }
            if (streamReader.getEventType() == XMLStreamReader.END_ELEMENT) {
                switch (streamReader.getLocalName()) {
                    case "HG-Entry":
                        homologeneEntry.setGenes(genes);
                        homologeneEntries.add(homologeneEntry);
                        genes = new ArrayList<>();
                        homologeneEntry = null;
                        break;
                    case "HG-Gene":
                        gene.setAliases(aliases);
                        gene.setDomains(domains);
                        genes.add(gene);
                        aliases = new ArrayList<>();
                        domains = new ArrayList<>();
                        gene = null;
                        break;
                    case "HG-Domain":
                        domains.add(domain);
                        domain = null;
                        break;
                    default:
                        break;
                }
            }
        }
        streamReader.close();
        inputStream.close();
        return homologeneEntries;
    }

    private static void addDoc(final IndexWriter writer, final HomologeneEntry entry) throws IOException {
        final Document doc = new Document();

        doc.add(new StringField(IndexFields.GROUP_ID.getFieldName(),
                String.valueOf(entry.getGroupId()), Field.Store.YES));

        doc.add(new StringField(IndexFields.VERSION.getFieldName(),
                String.valueOf(entry.getVersion()), Field.Store.YES));

        doc.add(new StringField(IndexFields.CAPTION.getFieldName(),
                String.valueOf(entry.getCaption()), Field.Store.YES));

        doc.add(new StringField(IndexFields.TAX_ID.getFieldName(),
                String.valueOf(entry.getTaxId()), Field.Store.YES));

        if (entry.getGenes() != null) {
            doc.add(new TextField(IndexFields.GENES.getFieldName(),
                    serializeGenes(entry.getGenes()), Field.Store.YES));
        }

        if (entry.getGenes() != null) {
            doc.add(new TextField(IndexFields.QUERY_FIELDS.getFieldName(),
                    serializeQueryFields(entry.getGenes()), Field.Store.YES));
        }
        writer.addDocument(doc);
    }

    @SneakyThrows
    private static List<Gene> deserializeGenes(final String encoded) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readValue(encoded, List.class);
    }

    private static String serializeGenes(final List<Gene> genes) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(genes);
    }

    private static String serializeQueryFields(final List<Gene> genes) {
        List<String> geneStrings = new ArrayList<>();
        for (Gene gene: genes) {
            geneStrings.add(gene.getSymbol() + (CollectionUtils.isEmpty(gene.getAliases()) ? ""
                    : GENE_FIELDS_LINE_DELIMITER + join(gene.getAliases(), GENE_FIELDS_LINE_DELIMITER)));
        }
        return join(geneStrings, GENE_FIELDS_LINE_DELIMITER);
    }
}
