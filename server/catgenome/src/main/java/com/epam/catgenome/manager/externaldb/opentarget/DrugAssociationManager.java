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
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.entity.externaldb.opentarget.DrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.UrlEntity;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TopDocs;
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.externaldb.opentarget.DiseaseManager.getDiseaseUrl;
import static com.epam.catgenome.util.IndexUtils.getByIdsQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;
import static com.epam.catgenome.util.Utils.DEFAULT_PAGE_SIZE;

@Service
@RequiredArgsConstructor
public class DrugAssociationManager {

    private static final String OPEN_TARGETS_DRUG_URL_PATTERN = "https://platform.opentargets.org/drug/%s";
    private final DiseaseManager diseaseManager;

    @Value("${opentargets.drug.association.index.directory}")
    private String indexDirectory;

    @Value("${targets.top.hits:10000}")
    private int targetsTopHits;

    public SearchResult<DrugAssociation> search(final DrugSearchRequest request)
            throws IOException, ParseException {
        final List<DrugAssociation> entries = new ArrayList<>();
        final SearchResult<DrugAssociation> searchResult = new SearchResult<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {

            final int page = (request.getPage() == null || request.getPage() <= 0) ? 1 : request.getPage();
            final int pageSize = (request.getPageSize() == null || request.getPage() <= 0) ? DEFAULT_PAGE_SIZE
                    : request.getPageSize();
            final int hits = page * pageSize;

            final Query query = getByTargetIdsQuery(request.getGeneIds());
            final Sort sort = getSort(request);

            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, hits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;

            final int from = (page - 1) * pageSize;
            final int to = Math.min(from + pageSize, scoreDocs.length);
            for (int i = from; i < to; i++) {
                Document doc = searcher.doc(scoreDocs[i].doc);
                DrugAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            searchResult.setItems(entries);
            searchResult.setTotalCount(topDocs.totalHits);
        }
        return searchResult;
    }

    public long totalCount(final List<String> ids) throws ParseException, IOException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            final Query query = getByTargetIdsQuery(ids);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            final TopDocs topDocs = searcher.search(query, targetsTopHits);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            final List<DrugAssociation> entries = new ArrayList<>();
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                DrugAssociation entry = entryFromDoc(doc);
                entries.add(entry);
            }
            return entries.stream().map(e -> e.getDrug().getId()).distinct().count();
        }
    }

    public void importData(final String path) throws IOException, ParseException {
        final File directory = getDirectory(path);
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexWriter writer = new IndexWriter(
                     index, new IndexWriterConfig(new StandardAnalyzer())
                     .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND))) {
            writer.deleteAll();
            for (File f: directory.listFiles()) {
                List<DrugAssociation> entries = readEntries(f);
                fillDiseaseNames(entries);
                for (DrugAssociation entry: entries) {
                    addDoc(writer, entry);
                }
            }
        }
    }

    private void fillDiseaseNames(final List<DrugAssociation> entries) throws ParseException, IOException {
        final List<String> diseaseIds = entries.stream()
                .map(r -> r.getDisease().getId())
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(diseaseIds)) {
            final List<Disease> diseases = diseaseManager.search(diseaseIds);
            final Map<String, UrlEntity> diseasesMap = diseases.stream()
                    .map(t -> new UrlEntity(t.getId(), t.getName(), t.getUrl()))
                    .collect(Collectors.toMap(UrlEntity::getId, Function.identity()));
            for (DrugAssociation r : entries) {
                if (diseasesMap.containsKey(r.getDisease().getId())) {
                    r.setDisease(diseasesMap.get(r.getDisease().getId()));
                }
            }
        }
    }

    private List<DrugAssociation> readEntries(final File path) throws IOException {
        final List<DrugAssociation> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        try (Reader reader = new FileReader(path); BufferedReader bufferedReader = new BufferedReader(reader)) {
            while ((line = bufferedReader.readLine()) != null) {
                try {
                    JsonNode jsonNodes = objectMapper.readTree(line);
                    DrugAssociation entry = entryFromJson(jsonNodes);
                    entries.add(entry);
                } catch (JsonProcessingException e) {
                    throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                }
            }
        }
        return entries;
    }

    private static Query getByTargetIdsQuery(final List<String> ids)
            throws ParseException {
        return getByIdsQuery(ids, IndexFields.GENE_ID.getFieldName());
    }

    @Getter
    private enum IndexFields {
        DRUG_ID("drugId"),
        DRUG_NAME("prefName"),
        DISEASE_ID("diseaseId"),
        DISEASE_NAME("diseaseName"),
        GENE_ID("geneId"),
        DRUG_TYPE("drugType"),
        MECHANISM_OF_ACTION("mechanismOfAction"),
        ACTION_TYPE("actionType"),
        PHASE("phase"),
        STATUS("status"),
        SOURCE("source"),
        SOURCE_URL("source_url");
        private final String fieldName;

        IndexFields(String fieldName) {
            this.fieldName = fieldName;
        }
    }

    private static void addDoc(final IndexWriter writer, final DrugAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DRUG_ID.getFieldName(), entry.getDrug().getId(), Field.Store.YES));

        doc.add(new TextField(IndexFields.DRUG_NAME.getFieldName(), entry.getDrug().getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.DRUG_NAME.getFieldName(),
                new BytesRef(entry.getDrug().getName())));

        doc.add(new TextField(IndexFields.DISEASE_ID.getFieldName(), entry.getDisease().getId(), Field.Store.YES));

        final String diseaseName = Optional.ofNullable(entry.getDisease().getName()).orElse("");
        doc.add(new TextField(IndexFields.DISEASE_NAME.getFieldName(), diseaseName, Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.DISEASE_NAME.getFieldName(), new BytesRef(diseaseName)));

        doc.add(new TextField(IndexFields.GENE_ID.getFieldName(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.GENE_ID.getFieldName(), new BytesRef(entry.getGeneId())));

        doc.add(new TextField(IndexFields.DRUG_TYPE.getFieldName(), entry.getDrugType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.DRUG_TYPE.getFieldName(), new BytesRef(entry.getDrugType())));

        doc.add(new TextField(IndexFields.MECHANISM_OF_ACTION.getFieldName(),
                entry.getMechanismOfAction(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.MECHANISM_OF_ACTION.getFieldName(),
                new BytesRef(entry.getMechanismOfAction())));

        doc.add(new TextField(IndexFields.ACTION_TYPE.getFieldName(), entry.getActionType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.ACTION_TYPE.getFieldName(), new BytesRef(entry.getActionType())));

        doc.add(new TextField(IndexFields.PHASE.getFieldName(), entry.getPhase(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.PHASE.getFieldName(), new BytesRef(entry.getPhase())));

        doc.add(new TextField(IndexFields.STATUS.getFieldName(), entry.getStatus(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.STATUS.getFieldName(), new BytesRef(entry.getStatus())));

        doc.add(new TextField(IndexFields.SOURCE.getFieldName(), entry.getSource().getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.SOURCE.getFieldName(), new BytesRef(entry.getSource().getName())));

        doc.add(new TextField(IndexFields.SOURCE_URL.getFieldName(), entry.getSource().getUrl(), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static DrugAssociation entryFromJson(final JsonNode jsonNodes) {
        final JsonNode urls = jsonNodes.at("/urls");
        final UrlEntity source = new UrlEntity();
        if (urls.isArray()) {
            Iterator<JsonNode> elements = urls.elements();
            if (elements.hasNext()) {
                JsonNode node = elements.next();
                source.setName(node.at("/niceName").asText());
                source.setUrl(node.at("/url").asText());
            }
        }
        final UrlEntity disease = new UrlEntity(jsonNodes.at("/diseaseId").asText());
        final UrlEntity drug = new UrlEntity(jsonNodes.at("/drugId").asText());
        drug.setName(jsonNodes.at("/prefName").asText());
        return DrugAssociation.builder()
                .geneId(jsonNodes.at("/targetId").asText())
                .disease(disease)
                .drug(drug)
                .drugType(jsonNodes.at("/drugType").asText())
                .mechanismOfAction(jsonNodes.at("/mechanismOfAction").asText())
                .actionType(jsonNodes.at("/mechanismOfAction").asText())
                .phase(jsonNodes.at("/phase").asText())
                .status(jsonNodes.at("/status").asText())
                .source(source)
                .build();
    }

    private static DrugAssociation entryFromDoc(final Document doc) {
        final UrlEntity source = new UrlEntity();
        source.setName(doc.getField(IndexFields.SOURCE.getFieldName()).stringValue());
        source.setUrl(doc.getField(IndexFields.SOURCE_URL.getFieldName()).stringValue());

        final UrlEntity disease = new UrlEntity();
        final String diseaseId = doc.getField(IndexFields.DISEASE_ID.getFieldName()).stringValue();
        disease.setId(diseaseId);
        disease.setName(doc.getField(IndexFields.DISEASE_NAME.getFieldName()).stringValue());
        disease.setUrl(getDiseaseUrl(diseaseId));

        final UrlEntity drug = new UrlEntity(doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue());
        drug.setName(doc.getField(IndexFields.DRUG_NAME.getFieldName()).stringValue());
        drug.setUrl(getDrugUrl(doc.getField(IndexFields.DRUG_ID.getFieldName()).stringValue()));

        return DrugAssociation.builder()
                .geneId(doc.getField(IndexFields.GENE_ID.getFieldName()).stringValue())
                .disease(disease)
                .drug(drug)
                .drugType(doc.getField(IndexFields.DRUG_TYPE.getFieldName()).stringValue())
                .mechanismOfAction(doc.getField(IndexFields.MECHANISM_OF_ACTION.getFieldName()).stringValue())
                .actionType(doc.getField(IndexFields.ACTION_TYPE.getFieldName()).stringValue())
                .phase(doc.getField(IndexFields.PHASE.getFieldName()).stringValue())
                .status(doc.getField(IndexFields.STATUS.getFieldName()).stringValue())
                .source(source)
                .build();
    }

    private static String getDrugUrl(final String drugId) {
        return String.format(OPEN_TARGETS_DRUG_URL_PATTERN, drugId);
    }

    private static Sort getSort(final DrugSearchRequest request) {
        final SortField sortField = request.getOrderBy() == null ?
                new SortField(DrugField.getDefault(), SortField.Type.STRING, false) :
                new SortField(request.getOrderBy().getName(), SortField.Type.STRING, request.isReverse());
        return new Sort(sortField);
    }
}
