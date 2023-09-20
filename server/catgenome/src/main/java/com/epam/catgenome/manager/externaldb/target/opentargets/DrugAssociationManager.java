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
import com.epam.catgenome.entity.externaldb.target.opentargets.Disease;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.UrlEntity;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import com.epam.catgenome.manager.index.Filter;
import com.epam.catgenome.manager.index.SearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.BytesRef;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.IndexUtils.getByTermQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class DrugAssociationManager extends AbstractAssociationManager<DrugAssociation> {

    @Value("${targets.opentargets.drugsDir:knownDrugsAggregated}")
    private String drugsDir;
    private final DiseaseManager diseaseManager;

    public DrugAssociationManager(final DiseaseManager diseaseManager,
                                  final @Value("${targets.index.directory}") String indexDirectory,
                                  final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "opentargets.drug.association").toString(), targetsTopHits);
        this.diseaseManager = diseaseManager;
    }

    public long totalCount(final List<String> ids) throws ParseException, IOException {
        final List<DrugAssociation> result = searchByGeneIds(ids);
        return result.stream().map(r -> r.getDrug().getId()).distinct().count();
    }

    public SearchResult<DrugAssociation> search(final SearchRequest request, final String diseaseId)
            throws ParseException, IOException {
        final BooleanQuery.Builder mainBuilder = new BooleanQuery.Builder();
        mainBuilder.add(getByTermQuery(diseaseId, DrugField.DISEASE_ID.name()), BooleanClause.Occur.MUST);
        if (request.getFilters() != null) {
            for (Filter filter: request.getFilters()) {
                addFieldQuery(mainBuilder, filter);
            }
        }
        return search(request, mainBuilder.build());
    }

    public DrugFieldValues getFieldValues(final List<String> geneIds) throws IOException, ParseException {
        final Query query = buildQuery(geneIds, null);
        final List<DrugAssociation> result = search(query, null);
        final List<String> drugTypes = result.stream()
            .map(DrugAssociation::getDrugType)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
        final List<String> mechanismOfActions = result.stream()
                .map(DrugAssociation::getMechanismOfAction)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> actionTypes = result.stream()
                .map(DrugAssociation::getActionType)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> phases = result.stream()
                .map(DrugAssociation::getPhase)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> statuses = result.stream()
                .map(DrugAssociation::getStatus)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        final List<String> sources = result.stream()
                .map(d -> d.getSource().getName())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return DrugFieldValues.builder()
                .drugTypes(drugTypes)
                .mechanismOfActions(mechanismOfActions)
                .actionTypes(actionTypes)
                .phases(phases)
                .statuses(statuses)
                .sources(sources)
                .build();
    }

    @Override
    public List<DrugAssociation> processEntries(final List<DrugAssociation> entries)
            throws ParseException, IOException {
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
        return entries;
    }

    @Override
    public List<DrugAssociation> readEntries(final String path) throws IOException {
        final Path drugsPath = Paths.get(path, drugsDir);
        final File directory = getDirectory(drugsPath.toString());
        final List<DrugAssociation> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        for (File f: directory.listFiles()) {
            try (Reader reader = new FileReader(f); BufferedReader bufferedReader = new BufferedReader(reader)) {
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
        }
        return entries;
    }

    @Override
    public String getDefaultSortField() {
        return DrugField.DRUG_NAME.name();
    }

    @Override
    public void addDoc(final IndexWriter writer, final DrugAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(DrugField.DRUG_ID.name(), entry.getDrug().getId(), Field.Store.YES));

        doc.add(new StringField(DrugField.DRUG_NAME_FLTR.name(), entry.getDrug().getName().toLowerCase(),
                Field.Store.NO));
        doc.add(new StringField(DrugField.DRUG_NAME.name(), entry.getDrug().getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DRUG_NAME.name(), new BytesRef(entry.getDrug().getName())));

        doc.add(new TextField(DrugField.DISEASE_ID.name(), entry.getDisease().getId(), Field.Store.YES));

        final String diseaseName = Optional.ofNullable(entry.getDisease().getName()).orElse("");
        doc.add(new StringField(DrugField.DISEASE_NAME_FLTR.name(), diseaseName.toLowerCase(), Field.Store.NO));
        doc.add(new StringField(DrugField.DISEASE_NAME.name(), diseaseName, Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DISEASE_NAME.name(), new BytesRef(diseaseName)));

        doc.add(new TextField(DrugField.GENE_ID.name(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.GENE_ID.name(), new BytesRef(entry.getGeneId())));

        doc.add(new StringField(DrugField.DRUG_TYPE.name(), entry.getDrugType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DRUG_TYPE.name(), new BytesRef(entry.getDrugType())));

        doc.add(new StringField(DrugField.MECHANISM_OF_ACTION.name(),
                entry.getMechanismOfAction(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.MECHANISM_OF_ACTION.name(),
                new BytesRef(entry.getMechanismOfAction())));

        doc.add(new StringField(DrugField.ACTION_TYPE.name(), entry.getActionType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.ACTION_TYPE.name(), new BytesRef(entry.getActionType())));

        doc.add(new StringField(DrugField.PHASE.name(), entry.getPhase(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.PHASE.name(), new BytesRef(entry.getPhase())));

        doc.add(new StringField(DrugField.STATUS.name(), entry.getStatus(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.STATUS.name(), new BytesRef(entry.getStatus())));

        doc.add(new StringField(DrugField.SOURCE.name(), entry.getSource().getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.SOURCE.name(), new BytesRef(entry.getSource().getName())));

        doc.add(new StringField(DrugField.SOURCE_URL.name(), entry.getSource().getUrl(), Field.Store.YES));
        writer.addDocument(doc);
    }

    @Override
    public DrugAssociation entryFromDoc(final Document doc) {
        final UrlEntity source = new UrlEntity();
        source.setName(doc.getField(DrugField.SOURCE.name()).stringValue());
        source.setUrl(doc.getField(DrugField.SOURCE_URL.name()).stringValue());

        final UrlEntity disease = new UrlEntity();
        final String diseaseId = doc.getField(DrugField.DISEASE_ID.name()).stringValue();
        disease.setId(diseaseId);
        disease.setName(doc.getField(DrugField.DISEASE_NAME.name()).stringValue());
        disease.setUrl(String.format(Disease.URL_PATTERN, diseaseId));

        final String drugId = doc.getField(DrugField.DRUG_ID.name()).stringValue();
        final UrlEntity drug = new UrlEntity(drugId);
        drug.setName(doc.getField(DrugField.DRUG_NAME.name()).stringValue());
        drug.setUrl(String.format(DrugAssociation.URL_PATTERN, drugId));

        return DrugAssociation.builder()
                .geneId(doc.getField(DrugField.GENE_ID.name()).stringValue())
                .disease(disease)
                .drug(drug)
                .drugType(doc.getField(DrugField.DRUG_TYPE.name()).stringValue())
                .mechanismOfAction(doc.getField(DrugField.MECHANISM_OF_ACTION.name()).stringValue())
                .actionType(doc.getField(DrugField.ACTION_TYPE.name()).stringValue())
                .phase(doc.getField(DrugField.PHASE.name()).stringValue())
                .status(doc.getField(DrugField.STATUS.name()).stringValue())
                .source(source)
                .build();
    }

    @Override
    public void addFieldQuery(final BooleanQuery.Builder builder, final Filter filter) {
        final BooleanQuery.Builder fieldBuilder = new BooleanQuery.Builder();
        for (String term: filter.getTerms()) {
            Query query;
            if (DrugField.DRUG_NAME.name().equals(filter.getField())) {
                query = buildPrefixQuery(DrugField.DRUG_NAME_FLTR.name(), term);
            } else if (DrugField.DISEASE_NAME.name().equals(filter.getField())) {
                query = buildPrefixQuery(DrugField.DISEASE_NAME_FLTR.name(), term);
            } else {
                query = buildTermQuery(filter.getField(), term);
            }
            fieldBuilder.add(query, BooleanClause.Occur.SHOULD);
        }
        builder.add(fieldBuilder.build(), BooleanClause.Occur.MUST);
    }

    private DrugAssociation entryFromJson(final JsonNode jsonNodes) {
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

    @Override
    public List<AssociationExportField<DrugAssociation>> getExportFields() {
        return Arrays.asList(DrugField.values());
    }
}
