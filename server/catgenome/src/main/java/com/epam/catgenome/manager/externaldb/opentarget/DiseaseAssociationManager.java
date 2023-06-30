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
package com.epam.catgenome.manager.externaldb.opentarget;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.opentarget.AssociationType;
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociation;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.buildTermQuery;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
public class DiseaseAssociationManager {

    private final String indexDirectory;
    private final int targetsTopHits;
    private final DiseaseManager diseaseManager;

    public DiseaseAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                     final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                     final DiseaseManager diseaseManager) {
        this.indexDirectory = Paths.get(indexDirectory, "opentargets.disease.association").toString();
        this.targetsTopHits = targetsTopHits;
        this.diseaseManager = diseaseManager;
    }

    public SearchResult<DiseaseAssociation> search(final DiseaseSearchRequest request)
            throws IOException, ParseException {
        final List<DiseaseAssociation> entries = new ArrayList<>();
        final SearchResult<DiseaseAssociation> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = buildQuery(request);
            final Sort sort = getSort(request);

            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                DiseaseAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    private List<DiseaseAssociation> searchAll(final Query query)
            throws IOException {
        final List<DiseaseAssociation> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                DiseaseAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
        }
        return entries;
    }

    public List<DiseaseAssociation> searchAll(final List<String> geneIds)
            throws IOException, ParseException {
        final Query query = getByGeneIdsQuery(geneIds);
        return searchAll(query);
    }

    public long totalCount(final List<String> geneIds) throws ParseException, IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByGeneIdsQuery(geneIds);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            final List<DiseaseAssociation> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                DiseaseAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(DiseaseAssociation::getDiseaseId).distinct().count();
        }
    }

    public void importData(final String path, final String overallPath) throws IOException, ParseException {
        final File directory = getDirectory(path);
        final File overallDirectory = getDirectory(overallPath);
        final List<DiseaseAssociation> allEntries = new ArrayList<>();
        for (File f: ArrayUtils.addAll(directory.listFiles(), overallDirectory.listFiles())) {
            List<DiseaseAssociation> entries = readEntries(f);
            allEntries.addAll(entries);
        }
        final List<DiseaseAssociation> processedEntries = processEntries(allEntries);
        fillDiseaseNames(processedEntries);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (DiseaseAssociation diseaseAssociation: processedEntries) {
                addDoc(writer, diseaseAssociation);
            }
        }
    }

    private List<DiseaseAssociation> processEntries(final List<DiseaseAssociation> entries) {
        final List<DiseaseAssociation> results = new ArrayList<>();
        final Map<AbstractMap.SimpleEntry<String, String>, List<DiseaseAssociation>> grouped = entries.stream()
                .collect(Collectors.groupingBy(entry ->
                        new AbstractMap.SimpleEntry<>(entry.getGeneId(), entry.getDiseaseId())));
        for (Map.Entry<AbstractMap.SimpleEntry<String, String>, List<DiseaseAssociation>> group : grouped.entrySet()) {
            DiseaseAssociation result = new DiseaseAssociation();
            AbstractMap.SimpleEntry<String, String> key = group.getKey();
            result.setGeneId(key.getKey());
            result.setDiseaseId(key.getValue());
            List<DiseaseAssociation> values = group.getValue();
            for (DiseaseAssociation value: values) {
                switch (value.getType()) {
                    case OVERALL:
                        result.setOverallScore(value.getScore());
                        break;
                    case GENETIC_ASSOCIATIONS:
                        result.setGeneticAssociationScore(value.getScore());
                        break;
                    case SOMATIC_MUTATIONS:
                        result.setSomaticMutationScore(value.getScore());
                        break;
                    case DRUGS:
                        result.setKnownDrugScore(value.getScore());
                        break;
                    case PATHWAYS:
                        result.setAffectedPathwayScore(value.getScore());
                        break;
                    case TEXT_MINING:
                        result.setLiteratureScore(value.getScore());
                        break;
                    case RNA_EXPRESSION:
                        result.setRnaExpressionScore(value.getScore());
                        break;
                    case ANIMAL_MODELS:
                        result.setAnimalModelScore(value.getScore());
                        break;
                    default:
                        break;
                }
            }
            results.add(result);
        }
        return results;
    }

    private void fillDiseaseNames(final List<DiseaseAssociation> entries) throws ParseException, IOException {
        final List<String> diseaseIds = entries.stream()
                .map(DiseaseAssociation::getDiseaseId)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(diseaseIds)) {
            final List<Disease> diseases = diseaseManager.search(diseaseIds);
            final Map<String, Disease> diseasesMap = diseases.stream()
                    .collect(Collectors.toMap(Disease::getId, Function.identity()));
            for (DiseaseAssociation r : entries) {
                if (diseasesMap.containsKey(r.getDiseaseId())) {
                    r.setDiseaseName(diseasesMap.get(r.getDiseaseId()).getName());
                }
            }
        }
    }

    private List<DiseaseAssociation> readEntries(final File path) throws IOException {
        final List<DiseaseAssociation> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    DiseaseAssociation entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    private static DiseaseAssociation entryFromJson(final JsonNode jsonNodes) {
        return DiseaseAssociation.builder()
                .geneId(jsonNodes.at("/targetId").asText())
                .diseaseId(jsonNodes.at("/diseaseId").asText())
                .type(jsonNodes.has("datatypeId") ?
                        AssociationType.getByName(jsonNodes.at("/datatypeId").asText()) :
                        AssociationType.OVERALL)
                .score(Float.parseFloat(jsonNodes.at("/score").asText()))
                .build();
    }

    @Getter
    private enum IndexField {
        GENE_ID("geneId"),
        DISEASE_ID("diseaseId"),
        DISEASE_NAME("diseaseName"),
        OVERALL_SCORE("overall_score"),
        GENETIC_ASSOCIATIONS_SCORE("genetic_association_score"),
        SOMATIC_MUTATIONS_SCORE("somatic_mutation_score"),
        DRUGS_SCORE("known_drug_score"),
        PATHWAYS_SCORE("affected_pathway_score"),
        TEXT_MINING_SCORE("literature_score"),
        RNA_EXPRESSION_SCORE("rna_expression_score"),
        ANIMAL_MODELS_SCORE("animal_model_score");
        private final String name;

        IndexField(String name) {
            this.name = name;
        }
    }

    private static void addDoc(final IndexWriter writer, final DiseaseAssociation entry) throws IOException {
        final Document doc = new Document();
        addStringField(entry.getGeneId(), doc, IndexField.GENE_ID);

        doc.add(new TextField(IndexField.DISEASE_ID.getName(), entry.getDiseaseId(), Field.Store.YES));

        addTextField(entry.getDiseaseName(), doc, IndexField.DISEASE_NAME);

        addFloatField(entry.getOverallScore(), doc, IndexField.OVERALL_SCORE);
        addFloatField(entry.getGeneticAssociationScore(), doc, IndexField.GENETIC_ASSOCIATIONS_SCORE);
        addFloatField(entry.getSomaticMutationScore(), doc, IndexField.SOMATIC_MUTATIONS_SCORE);
        addFloatField(entry.getKnownDrugScore(), doc, IndexField.DRUGS_SCORE);
        addFloatField(entry.getAffectedPathwayScore(), doc, IndexField.PATHWAYS_SCORE);
        addFloatField(entry.getLiteratureScore(), doc, IndexField.TEXT_MINING_SCORE);
        addFloatField(entry.getRnaExpressionScore(), doc, IndexField.RNA_EXPRESSION_SCORE);
        addFloatField(entry.getAnimalModelScore(), doc, IndexField.ANIMAL_MODELS_SCORE);
        writer.addDocument(doc);
    }

    private static void addStringField(final String entry, final Document doc, final IndexField field) {
        if (entry != null) {
            doc.add(new StringField(field.getName(), entry, Field.Store.YES));
            doc.add(new SortedDocValuesField(field.getName(), new BytesRef(entry)));
        }
    }

    private static void addTextField(final String entry, final Document doc, final IndexField field) {
        if (entry != null) {
            doc.add(new TextField(field.getName(), entry, Field.Store.YES));
            doc.add(new SortedDocValuesField(field.getName(), new BytesRef(entry)));
        }
    }

    private static void addFloatField(final Float entry, final Document doc, final IndexField field) {
        if (entry != null) {
            doc.add(new FloatPoint(field.getName(), entry));
            doc.add(new StoredField(field.getName(), entry));
            doc.add(new FloatDocValuesField(field.getName(), entry));
        }
    }

    private static DiseaseAssociation entryFromDoc(final Document doc) {
        final DiseaseAssociation d = DiseaseAssociation.builder()
                .geneId(doc.getField(IndexField.GENE_ID.getName()).stringValue())
                .diseaseId(doc.getField(IndexField.DISEASE_ID.getName()).stringValue())
                .build();
        if (doc.getField(IndexField.DISEASE_NAME.getName()) != null) {
            d.setDiseaseName(doc.getField(IndexField.DISEASE_NAME.getName()).stringValue());
        }
        if (doc.getField(IndexField.OVERALL_SCORE.getName()) != null) {
            d.setOverallScore(getScore(doc, IndexField.OVERALL_SCORE));
        }
        if (doc.getField(IndexField.GENETIC_ASSOCIATIONS_SCORE.getName()) != null) {
            d.setGeneticAssociationScore(getScore(doc, IndexField.GENETIC_ASSOCIATIONS_SCORE));
        }
        if (doc.getField(IndexField.SOMATIC_MUTATIONS_SCORE.getName()) != null) {
            d.setSomaticMutationScore(getScore(doc, IndexField.SOMATIC_MUTATIONS_SCORE));
        }
        if (doc.getField(IndexField.DRUGS_SCORE.getName()) != null) {
            d.setKnownDrugScore(getScore(doc, IndexField.DRUGS_SCORE));
        }
        if (doc.getField(IndexField.PATHWAYS_SCORE.getName()) != null) {
            d.setAffectedPathwayScore(getScore(doc, IndexField.PATHWAYS_SCORE));
        }
        if (doc.getField(IndexField.TEXT_MINING_SCORE.getName()) != null) {
            d.setLiteratureScore(getScore(doc, IndexField.TEXT_MINING_SCORE));
        }
        if (doc.getField(IndexField.RNA_EXPRESSION_SCORE.getName()) != null) {
            d.setRnaExpressionScore(getScore(doc, IndexField.RNA_EXPRESSION_SCORE));
        }
        if (doc.getField(IndexField.ANIMAL_MODELS_SCORE.getName()) != null) {
            d.setAnimalModelScore(getScore(doc, IndexField.ANIMAL_MODELS_SCORE));
        }
        return d;
    }

    private static float getScore(final Document doc, final IndexField field) {
        return  doc.getField(field.getName()).numericValue().floatValue();
    }

    private static Query getByGeneIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, IndexField.GENE_ID.getName());
    }

    private static Sort getSort(final DiseaseSearchRequest request) {
        if (request.getOrderBy() == null) {
            return new Sort(new SortField(DiseaseField.getDefault(), SortField.Type.FLOAT, true));
        }
        final SortField.Type sortType = request.getOrderBy().equals(DiseaseField.DISEASE_NAME) ||
                request.getOrderBy().equals(DiseaseField.GENE_ID) ? SortField.Type.STRING : SortField.Type.FLOAT;
        return new Sort(new SortField(request.getOrderBy().getName(), sortType, request.isReverse()));
    }

    private static Query buildQuery(final DiseaseSearchRequest request) throws ParseException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        final BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (String geneId : request.getGeneIds()) {
            builder.add(buildTermQuery(geneId, DiseaseField.GENE_ID.getName()), BooleanClause.Occur.SHOULD);
        }
        mainBuilder.add(builder.build(), BooleanClause.Occur.MUST);

        if (request.getFilterBy() != null && request.getTerm() != null &&
                DiseaseField.DISEASE_NAME.equals(request.getFilterBy())) {
            final StandardAnalyzer analyzer = new StandardAnalyzer();
            final Query query = new QueryParser(request.getFilterBy().getName(), analyzer).parse(request.getTerm());
            mainBuilder.add(query, BooleanClause.Occur.MUST);
        }
        return mainBuilder.build();
    }
}
