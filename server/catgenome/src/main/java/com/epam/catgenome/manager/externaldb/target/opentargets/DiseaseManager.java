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
package com.epam.catgenome.manager.externaldb.target.opentargets;

import com.epam.catgenome.constant.MessagesConstants;
import com.epam.catgenome.entity.externaldb.target.opentargets.BareDisease;
import com.epam.catgenome.entity.externaldb.target.opentargets.Disease;
import com.epam.catgenome.entity.externaldb.target.opentargets.UrlEntity;
import com.epam.catgenome.manager.index.AbstractIndexManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.deserialize;
import static com.epam.catgenome.util.IndexUtils.getByTermQuery;
import static com.epam.catgenome.util.IndexUtils.getField;
import static com.epam.catgenome.util.IndexUtils.serialize;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class DiseaseManager extends AbstractIndexManager<Disease> {

    @Value("${targets.opentargets.diseasesDir:diseases}")
    private String diseasesDir;
    public DiseaseManager(final @Value("${targets.index.directory}") String indexDirectory,
                          final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "opentargets.disease").toString(), targetsTopHits);
    }

    public List<Disease> search(final List<String> ids) throws ParseException, IOException {
        return search(ids, IndexFields.DISEASE_ID.name());
    }

    public List<BareDisease> search() throws IOException {
        final List<BareDisease> diseases = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            int numDocs = indexReader.numDocs();
            for (int docId = 0; docId < numDocs; docId++) {
                Document doc = indexReader.document(docId);
                BareDisease entry = bareEntryFromDoc(doc);
                diseases.add(entry);
            }
        }
        return diseases;
    }

    public Disease searchById(final String diseaseId) throws IOException, ParseException {
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            Query query = getByTermQuery(diseaseId, IndexFields.DISEASE_ID.name());
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, 1);
            if (topDocs.totalHits > 0) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[0];
                Document doc = searcher.doc(scoreDoc.doc);
                return fullEntryFromDoc(doc);
            }
        }
        return null;
    }

    public Map<String, String> search(final String name) throws IOException, ParseException {
        final Map<String, String> entries = new LinkedHashMap<>();
        final List<SortField> sortFields = Collections.singletonList(new SortField(IndexFields.NAME.name(),
                SortField.Type.STRING, false));
        final Sort sort = new Sort(sortFields.toArray(new SortField[1]));
        final Query query = getByTermQuery(name, IndexFields.NAME.name());
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, topHits, sort);
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (ScoreDoc scoreDoc : scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                entries.put(getField(doc, IndexFields.DISEASE_ID.name()), getField(doc, IndexFields.NAME.name()));
            }
        }
        return entries;
    }

    @Override
    public List<Disease> readEntries(final String path) throws IOException {
        final Path diseasesPath = Paths.get(path, diseasesDir);
        final File directory = getDirectory(diseasesPath.toString());
        final List<Disease> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        for (File f: directory.listFiles()) {
            try (FileInputStream fis = new FileInputStream(f);
                 InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(isr)) {
                while ((line = bufferedReader.readLine()) != null) {
                    try {
                        JsonNode jsonNodes = objectMapper.readTree(line);
                        Disease entry = entryFromJson(jsonNodes);
                        entries.add(entry);
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException(MessagesConstants.ERROR_INCORRECT_JSON_FORMAT);
                    }
                }
            }
        }
        return entries;
    }

    @Override
    public String getDefaultSortField() {
        return null;
    }

    @Override
    public List<Disease> processEntries(List<Disease> entries) throws IOException, ParseException {
        return entries;
    }

    @Override
    public Disease entryFromDoc(final Document doc) {
        final List<String> therapeuticAreasStr = deserialize(getField(doc,
                IndexFields.THERAPEUTIC_AREA_IDS.name()));
        final List<UrlEntity> therapeuticAreas = therapeuticAreasStr.stream()
                .map(UrlEntity::new)
                .collect(Collectors.toList());
        final List<String> parents = deserialize(getField(doc, IndexFields.PARENTS.name()));
        return Disease.builder()
                .id(getField(doc, IndexFields.DISEASE_ID.name()))
                .name(getField(doc, IndexFields.NAME.name()))
                .url(String.format(Disease.URL_PATTERN, getField(doc, IndexFields.DISEASE_ID.name())))
                .parents(parents)
                .isTherapeuticArea(CollectionUtils.isEmpty(parents) || parents.stream().allMatch(StringUtils::isBlank))
                .therapeuticAreas(therapeuticAreas)
                .build();
    }

    private Disease fullEntryFromDoc(final Document doc) {
        final List<String> synonyms = deserialize(getField(doc, IndexFields.SYNONYMS.name()));
        return Disease.builder()
                .id(getField(doc, IndexFields.DISEASE_ID.name()))
                .name(getField(doc, IndexFields.NAME.name()))
                .url(String.format(Disease.URL_PATTERN, getField(doc, IndexFields.DISEASE_ID.name())))
                .description(getField(doc, IndexFields.DESCRIPTION.name()))
                .synonyms(synonyms)
                .build();
    }

    @Override
    public void addDoc(final IndexWriter writer, final Disease entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(IndexFields.DISEASE_ID.name(), entry.getId(), Field.Store.YES));

        doc.add(new TextField(IndexFields.NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(IndexFields.NAME.name(), new BytesRef(entry.getName())));

        doc.add(new StringField(IndexFields.DESCRIPTION.name(), entry.getDescription(), Field.Store.YES));
        doc.add(new StringField(IndexFields.SYNONYMS.name(), serialize(entry.getSynonyms()), Field.Store.YES));

        doc.add(new StringField(IndexFields.PARENTS.name(), serialize(entry.getParents()), Field.Store.YES));
        final List<String> therapeuticAreaIds = entry.getTherapeuticAreas().stream()
                .map(UrlEntity::getId)
                .collect(Collectors.toList());
        doc.add(new StringField(IndexFields.THERAPEUTIC_AREA_IDS.name(),
                serialize(therapeuticAreaIds), Field.Store.YES));
        writer.addDocument(doc);
    }

    private static Disease entryFromJson(final JsonNode jsonNodes) {
        final JsonNode therapeuticAreasNode = jsonNodes.at("/therapeuticAreas");
        final List<UrlEntity> therapeuticAreas = new ArrayList<>();
        UrlEntity urlEntity;
        if (therapeuticAreasNode.isArray()) {
            Iterator<JsonNode> node = therapeuticAreasNode.elements();
            while (node.hasNext()) {
                urlEntity = new UrlEntity(node.next().asText());
                therapeuticAreas.add(urlEntity);
            }
        }
        final JsonNode parentsNode = jsonNodes.at("/parents");
        final List<String> parents = new ArrayList<>();
        if (parentsNode.isArray()) {
            Iterator<JsonNode> node = parentsNode.elements();
            while (node.hasNext()) {
                parents.add(node.next().asText());
            }
        }
        final List<String> synonyms = new ArrayList<>();
        final JsonNode exactSynonymsNode = jsonNodes.at("/synonyms/hasExactSynonym");
        if (exactSynonymsNode.isArray()) {
            Iterator<JsonNode> node = exactSynonymsNode.elements();
            while (node.hasNext()) {
                synonyms.add(node.next().asText());
            }
        }
        final JsonNode relatedSynonymsNode = jsonNodes.at("/synonyms/hasRelatedSynonym");
        if (relatedSynonymsNode.isArray()) {
            Iterator<JsonNode> node = relatedSynonymsNode.elements();
            while (node.hasNext()) {
                synonyms.add(node.next().asText());
            }
        }
        return Disease.builder()
                .id(jsonNodes.at("/id").asText())
                .name(jsonNodes.at("/name").asText())
                .description(jsonNodes.at("/description").asText())
                .synonyms(synonyms)
                .therapeuticAreas(therapeuticAreas)
                .parents(parents)
                .build();
    }

    private static BareDisease bareEntryFromDoc(final Document doc) {
        return BareDisease.builder()
                .id(getField(doc, IndexFields.DISEASE_ID.name()))
                .parents(deserialize(getField(doc, IndexFields.PARENTS.name())))
                .build();
    }

    @Getter
    private enum IndexFields {
        DISEASE_ID,
        NAME,
        DESCRIPTION,
        SYNONYMS,
        THERAPEUTIC_AREA_IDS,
        PARENTS;
    }
}
