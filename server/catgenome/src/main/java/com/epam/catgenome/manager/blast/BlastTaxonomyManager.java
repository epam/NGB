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
package com.epam.catgenome.manager.blast;

import com.epam.catgenome.manager.blast.dto.BlastTaxonomy;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
public class BlastTaxonomyManager {

    private static final String AUTHORITY = "authority";
    private static final String COMMON_NAME = "common name";
    private static final String SCIENTIFIC_NAME = "scientific name";
    private static final String TAXONOMY_DELIMITER = "\\|";
    private static final List<String> RECORDS_TO_BE_EXCLUDED = Arrays.asList("type material", "in-part");
    private static final int TOP_HITS = 10;

    @Value("${taxonomy.index.directory}")
    private String taxonomyIndexDirectory;

    public List<BlastTaxonomy> searchOrganisms(final String term, final String taxonomyIndexDirectory)
            throws IOException, ParseException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        String[] fields = new String[] {TaxonomyIndexFields.COMMON_NAME.getFieldName(),
                TaxonomyIndexFields.SCIENTIFIC_NAME.getFieldName(),
                TaxonomyIndexFields.SYNONYMS.getFieldName()
        };
        Query query = new MultiFieldQueryParser(fields, analyzer).parse(term);
        List<BlastTaxonomy> organisms = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, TOP_HITS);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                organisms.add(new BlastTaxonomy(getTaxId(doc),
                        getScientificName(doc),
                        getCommonName(doc),
                        getSynonyms(doc)));
            }
        }
        return organisms;
    }

    public BlastTaxonomy searchOrganismById(final long taxId, final String taxonomyIndexDirectory)
            throws IOException, ParseException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        Query query = new QueryParser(TaxonomyIndexFields.TAX_ID.getFieldName(), analyzer)
                .parse(String.valueOf(taxId));
        BlastTaxonomy blastTaxonomy;
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 1);
            ScoreDoc scoreDoc = topDocs.scoreDocs.length > 0 ? topDocs.scoreDocs[0] : null;
            Document doc = scoreDoc != null ? searcher.doc(scoreDoc.doc) : null;
            blastTaxonomy = doc == null ? null : new BlastTaxonomy(getTaxId(doc),
                    getCommonName(doc),
                    getScientificName(doc),
                    getSynonyms(doc));
        }
        return blastTaxonomy;
    }

    @Nullable
    private String getScientificName(final Document doc) {
        return doc.getField(TaxonomyIndexFields.SCIENTIFIC_NAME.getFieldName()) == null ? null
                : doc.getField(TaxonomyIndexFields.SCIENTIFIC_NAME.getFieldName()).stringValue();
    }

    @Nullable
    private List<String> getSynonyms(final Document doc) {
        return doc.getField(TaxonomyIndexFields.SYNONYMS.getFieldName()) == null ? null
                : deserialize(doc.getField(TaxonomyIndexFields.SYNONYMS.getFieldName()).stringValue());
    }

    @Nullable
    private String getCommonName(final Document doc) {
        return doc.getField(TaxonomyIndexFields.COMMON_NAME.getFieldName()) == null ? null
                : doc.getField(TaxonomyIndexFields.COMMON_NAME.getFieldName()).stringValue();
    }

    private long getTaxId(final Document doc) {
        return Long.parseLong(doc.getField(TaxonomyIndexFields.TAX_ID.getFieldName()).stringValue());
    }

    public void writeLuceneTaxonomyIndex(final String filename, final String taxonomyIndexDirectory)
            throws IOException, ParseException {
        List<BlastTaxonomy> organisms = readTaxonomy(filename);
        StandardAnalyzer analyzer = new StandardAnalyzer();
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexWriter writer = new IndexWriter(index,
                     new IndexWriterConfig(analyzer).setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (BlastTaxonomy o : organisms) {
                addDoc(writer, o);
            }
        }
    }

    @AllArgsConstructor
    @Getter
    private static class TaxonomyRecord {
        long taxId;
        String line;
    }

    private enum TaxonomyIndexFields {
        TAX_ID("taxId"),
        COMMON_NAME("commonName"),
        SCIENTIFIC_NAME("scientificName"),
        SYNONYMS("synonyms");

        private final String fieldName;

        TaxonomyIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }

        public String getFieldName() {
            return fieldName;
        }
    }

    public List<BlastTaxonomy> readTaxonomy(final String filename) {
        List<BlastTaxonomy> organisms = new ArrayList<>();
        List<TaxonomyRecord> taxonomyRecords = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            long taxId;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(TAXONOMY_DELIMITER);
                taxId = Long.parseLong(fields[0].trim());
                taxonomyRecords.add(new TaxonomyRecord(taxId, line));
            }
        }
        catch (IOException|NumberFormatException e) {
            log.error(e.getMessage());
        }
        Map<Long, List<TaxonomyRecord>> taxMap = taxonomyRecords
                .stream()
                .collect(Collectors.groupingBy(TaxonomyRecord::getTaxId));
        long taxId;
        String name;
        String commonName = null;
        String scientificName = null;
        String authority = null;
        for (Map.Entry<Long, List<TaxonomyRecord>> entry : taxMap.entrySet()) {
            List<String> synonyms = new ArrayList<>();
            taxId = entry.getKey();
            List<String> lines = entry.getValue().stream().map(TaxonomyRecord::getLine).collect(Collectors.toList());
            for (String line: lines) {
                name = line.split(TAXONOMY_DELIMITER)[1].trim();
                if (commonName == null && line.contains(COMMON_NAME)) {
                    commonName = name;
                } else if (scientificName == null && line.contains(SCIENTIFIC_NAME)) {
                    scientificName = name;
                } else if (line.contains(AUTHORITY)) {
                    authority = name;
                    synonyms.add(name);
                } else if (!excludeLine(line)) {
                    synonyms.add(name);
                }
            }
            if (authority != null && (commonName != null || scientificName != null)) {
                organisms.add(new BlastTaxonomy(taxId, commonName, scientificName, synonyms));
            }
            commonName = null;
            scientificName = null;
            authority = null;
        }
        return organisms;
    }

    private static void addDoc(final IndexWriter w, final BlastTaxonomy taxonomy) throws IOException {
        Document doc = new Document();
        doc.add(new StringField(TaxonomyIndexFields.TAX_ID.getFieldName(),
                String.valueOf(taxonomy.getTaxId()), Field.Store.YES));
        if (taxonomy.getCommonName() != null) {
            doc.add(new TextField(TaxonomyIndexFields.COMMON_NAME.getFieldName(),
                    taxonomy.getCommonName(), Field.Store.YES));
        }
        if (taxonomy.getScientificName() != null) {
            doc.add(new TextField(TaxonomyIndexFields.SCIENTIFIC_NAME.getFieldName(),
                    taxonomy.getScientificName(), Field.Store.YES));
        }
        if (!CollectionUtils.isEmpty(taxonomy.getSynonyms())) {
            doc.add(new TextField(TaxonomyIndexFields.SYNONYMS.getFieldName(),
                    serialize(taxonomy.getSynonyms()), Field.Store.YES));
        }
        w.addDocument(doc);
    }

    private static List<String> deserialize(final String encoded) {
        return Arrays.asList(encoded.split(TAXONOMY_DELIMITER));
    }

    private static boolean excludeLine(final String line) {
        return RECORDS_TO_BE_EXCLUDED.stream().anyMatch(line::contains);
    }

    private static String serialize(final List<String> strings) {
        return join(strings, "|");
    }
}
