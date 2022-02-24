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
package com.epam.catgenome.manager.externaldb.taxonomy;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.SneakyThrows;
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
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.deserialize;
import static com.epam.catgenome.util.IndexUtils.serialize;
import static org.apache.commons.lang3.StringUtils.join;

@Service
@Slf4j
public class TaxonomyManager {

    private static final String COMMON_NAME = "common name";
    private static final String SCIENTIFIC_NAME = "scientific name";
    private static final String TAXONOMY_TOKEN_DELIMITER_PATTERN = "\\|";
    private static final List<String> RECORDS_TO_BE_EXCLUDED = Arrays.asList("type material", "in-part");
    private static final String TAXONOMY_TERM_SPLIT_TOKEN = " ";

    @Value("${taxonomy.index.directory}")
    private String taxonomyIndexDirectory;

    @Value("${taxonomy.top.hits:10}")
    private int taxonomyTopHits;

    public List<Taxonomy> searchOrganisms(final String terms) throws IOException, ParseException {
        final List<Taxonomy> organisms = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
            IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(buildTaxonomySearchQuery(terms), taxonomyTopHits);
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                organisms.add(
                    Taxonomy.builder()
                        .taxId(getTaxId(doc))
                        .scientificName(getScientificName(doc))
                        .commonName(getCommonName(doc))
                        .synonyms(getSynonyms(doc))
                        .build()
                );
            }
        }
        return organisms;
    }

    @SneakyThrows
    public Taxonomy searchOrganismById(final long taxId) {
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final Query query = new QueryParser(TaxonomyIndexFields.TAX_ID.getFieldName(), analyzer)
                .parse(String.valueOf(taxId));
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 1);
            ScoreDoc scoreDoc = topDocs.scoreDocs.length > 0 ? topDocs.scoreDocs[0] : null;
            Document doc = scoreDoc != null ? searcher.doc(scoreDoc.doc) : null;
            return doc == null ? null
                    :  Taxonomy.builder().taxId(getTaxId(doc))
                            .scientificName(getScientificName(doc))
                            .commonName(getCommonName(doc))
                            .synonyms(getSynonyms(doc))
                            .build();
        }
    }

    @SneakyThrows
    public List<Taxonomy> searchOrganismsByIds(final Set<Long> taxIds) {
        final List<Taxonomy> organisms = new ArrayList<>();
        final StandardAnalyzer analyzer = new StandardAnalyzer();
        final QueryParser queryParser = new QueryParser(TaxonomyIndexFields.TAX_ID.getFieldName(), analyzer);
        queryParser.setDefaultOperator(QueryParser.Operator.OR);
        Query query = queryParser.parse(join(taxIds, TAXONOMY_TERM_SPLIT_TOKEN));

        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, taxIds.size());
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                organisms.add(
                        Taxonomy.builder()
                                .taxId(getTaxId(doc))
                                .scientificName(getScientificName(doc))
                                .commonName(getCommonName(doc))
                                .synonyms(getSynonyms(doc))
                                .build()
                );
            }
        }
        return organisms;
    }

    public void writeLuceneTaxonomyIndex(final String taxonomyFilePath) throws IOException, ParseException {
        try (Directory index = new SimpleFSDirectory(Paths.get(taxonomyIndexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (Taxonomy taxonomyEntry: readTaxonomy(taxonomyFilePath)) {
                addDoc(writer, taxonomyEntry);
            }
        }
    }

    public List<Taxonomy> readTaxonomy(final String path) {
        return readTaxonomyLines(path).stream().collect(Collectors.groupingBy(TaxonomyRecord::getTaxId))
                .entrySet().stream()
                .map(entry -> processTaxonomyId(entry.getKey(), entry.getValue()))
                .filter(Objects::nonNull)
                .filter(this::taxonomyIdIsComplete)
                .collect(Collectors.toList());
    }

    private Query buildTaxonomySearchQuery(final String terms) {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        for (String term : terms.split(TAXONOMY_TERM_SPLIT_TOKEN)) {
            mainBuilder.add(
                new BooleanQuery.Builder()
                    .add(buildPrefixQuery(term, TaxonomyIndexFields.COMMON_NAME), BooleanClause.Occur.SHOULD)
                    .add(buildPrefixQuery(term, TaxonomyIndexFields.SCIENTIFIC_NAME), BooleanClause.Occur.SHOULD)
                    .add(buildPrefixQuery(term, TaxonomyIndexFields.SYNONYMS), BooleanClause.Occur.SHOULD)
                    .build(),
                BooleanClause.Occur.MUST
            );
        }
        return mainBuilder.build();
    }

    private PrefixQuery buildPrefixQuery(final String term, final TaxonomyIndexFields field) {
        return new PrefixQuery(new Term(field.getFieldName(), term.toLowerCase()));
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

    @Getter
    @AllArgsConstructor
    private static class TaxonomyRecord {
        long taxId;
        String line;
    }

    @Getter
    private enum TaxonomyIndexFields {
        TAX_ID("taxId"),
        COMMON_NAME("commonName"),
        SCIENTIFIC_NAME("scientificName"),
        SYNONYMS("synonyms");

        private final String fieldName;

        TaxonomyIndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private boolean taxonomyIdIsComplete(final Taxonomy taxonomy) {
        return (taxonomy.getCommonName() != null || taxonomy.getScientificName() != null);
    }

    private Taxonomy processTaxonomyId(final Long taxId, final List<TaxonomyRecord> records) {
        final List<String> synonyms = new ArrayList<>();
        final Taxonomy.TaxonomyBuilder taxonomyBuilder = Taxonomy.builder()
                .taxId(taxId).synonyms(synonyms);
        for (final TaxonomyRecord record : records) {
            final String name = record.line.split(TAXONOMY_TOKEN_DELIMITER_PATTERN)[1].trim();
            if (record.line.contains(COMMON_NAME)) {
                taxonomyBuilder.commonName(name);
            } else if (record.line.contains(SCIENTIFIC_NAME)) {
                taxonomyBuilder.scientificName(name);
            } else if (!excludeLine(record.line)) {
                synonyms.add(name);
            }
        }
        return taxonomyBuilder.build();
    }

    private List<TaxonomyRecord> readTaxonomyLines(final String path) {
        try {
            return Files.lines(Paths.get(path))
                    .map(tl -> new TaxonomyRecord(
                            Long.parseLong(tl.split(TAXONOMY_TOKEN_DELIMITER_PATTERN)[0].trim()), tl))
                    .collect(Collectors.toList());
        } catch (IOException | NumberFormatException e) {
            log.error(e.getMessage());
            return Collections.emptyList();
        }
    }

    private static void addDoc(final IndexWriter writer, final Taxonomy taxonomy) throws IOException {
        final Document doc = new Document();
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
        writer.addDocument(doc);
    }

    private static boolean excludeLine(final String line) {
        return RECORDS_TO_BE_EXCLUDED.stream().anyMatch(line::contains);
    }
}
