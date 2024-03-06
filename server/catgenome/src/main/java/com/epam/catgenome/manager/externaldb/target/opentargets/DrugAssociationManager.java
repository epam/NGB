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
import com.epam.catgenome.entity.externaldb.target.opentargets.TargetDetails;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.externaldb.target.opentargets.Phase.getPhase;
import static com.epam.catgenome.util.IndexUtils.getByTermQuery;
import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class DrugAssociationManager extends AbstractAssociationManager<DrugAssociation> {

    @Value("${targets.opentargets.drugsDir:knownDrugsAggregated}")
    private String drugsDir;
    private final DiseaseManager diseaseManager;
    private final TargetDetailsManager targetDetailsManager;

    public DrugAssociationManager(final DiseaseManager diseaseManager,
                                  final TargetDetailsManager targetDetailsManager,
                                  final @Value("${targets.index.directory}") String indexDirectory,
                                  final @Value("${targets.top.hits:10000}") int targetsTopHits) {
        super(Paths.get(indexDirectory, "opentargets.drug.association").toString(), targetsTopHits);
        this.diseaseManager = diseaseManager;
        this.targetDetailsManager = targetDetailsManager;
    }

    public Pair<Long, Long> totalCount(final String diseaseId) throws ParseException, IOException {
        final List<DrugAssociation> result = search(diseaseId);
        return Pair.of((long) result.size(), result.stream().map(DrugAssociation::getId).distinct().count());
    }

    public DrugFieldValues getFieldValues(final List<String> geneIds) throws IOException, ParseException {
        final Query query = getByGeneIdsQuery(geneIds);
        return getFieldValues(query);
    }

    public DrugFieldValues getFieldValues(final String diseaseId) throws IOException, ParseException {
        final Query query = getByTermQuery(diseaseId, DrugField.DISEASE_ID.name());
        return getFieldValues(query);
    }

    @Override
    public List<DrugAssociation> processEntries(final List<DrugAssociation> entries)
            throws ParseException, IOException {
        fillDiseases(entries);
        fillGenes(entries);
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
    public SortField getDefaultSortField() {
        return new SortField(DrugField.DRUG_NAME.name(), SortField.Type.STRING, false);
    }

    @Override
    public void addDoc(final IndexWriter writer, final DrugAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new StringField(DrugField.DRUG_ID.name(), entry.getId(), Field.Store.YES));

        doc.add(new TextField(DrugField.DRUG_NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DRUG_NAME.name(), new BytesRef(entry.getName())));

        doc.add(new TextField(DrugField.DISEASE_ID.name(), entry.getDisease().getId(), Field.Store.YES));

        final String diseaseName = Optional.ofNullable(entry.getDisease().getName()).orElse("");
        doc.add(new TextField(DrugField.DISEASE_NAME.name(), diseaseName, Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DISEASE_NAME.name(), new BytesRef(diseaseName)));

        doc.add(new TextField(DrugField.GENE_ID.name(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.GENE_ID.name(), new BytesRef(entry.getGeneId())));

        final String geneSymbol = Optional.ofNullable(entry.getGeneSymbol()).orElse("");
        doc.add(new TextField(DrugField.GENE_SYMBOL.name(), geneSymbol, Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.GENE_SYMBOL.name(), new BytesRef(geneSymbol)));

        final String geneName = Optional.ofNullable(entry.getGeneName()).orElse("");
        doc.add(new TextField(DrugField.GENE_NAME.name(), geneName, Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.GENE_NAME.name(), new BytesRef(geneName)));

        doc.add(new StringField(DrugField.DRUG_TYPE.name(), entry.getDrugType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.DRUG_TYPE.name(), new BytesRef(entry.getDrugType())));

        doc.add(new StringField(DrugField.MECHANISM_OF_ACTION.name(),
                entry.getMechanismOfAction(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.MECHANISM_OF_ACTION.name(),
                new BytesRef(entry.getMechanismOfAction())));

        doc.add(new StringField(DrugField.ACTION_TYPE.name(), entry.getActionType(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.ACTION_TYPE.name(), new BytesRef(entry.getActionType())));

        final String phase = getPhase(entry.getPhase());
        doc.add(new StringField(DrugField.PHASE.name(), phase, Field.Store.YES));
        doc.add(new SortedDocValuesField(DrugField.PHASE.name(), new BytesRef(phase)));

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

        final String id = doc.getField(DrugField.DRUG_ID.name()).stringValue();
        return DrugAssociation.builder()
                .id(id)
                .name(doc.getField(DrugField.DRUG_NAME.name()).stringValue())
                .url(String.format(DrugAssociation.URL_PATTERN, id))
                .geneId(doc.getField(DrugField.GENE_ID.name()).stringValue())
                .disease(disease)
                .drugType(doc.getField(DrugField.DRUG_TYPE.name()).stringValue())
                .mechanismOfAction(doc.getField(DrugField.MECHANISM_OF_ACTION.name()).stringValue())
                .actionType(doc.getField(DrugField.ACTION_TYPE.name()).stringValue())
                .phase(doc.getField(DrugField.PHASE.name()).stringValue())
                .status(doc.getField(DrugField.STATUS.name()).stringValue())
                .source(source)
                .build();
    }

    @Override
    public DrugAssociation entryFromDocDiseaseView(final Document doc) {
        final UrlEntity source = new UrlEntity();
        source.setName(doc.getField(DrugField.SOURCE.name()).stringValue());
        source.setUrl(doc.getField(DrugField.SOURCE_URL.name()).stringValue());

        final String drugId = doc.getField(DrugField.DRUG_ID.name()).stringValue();
        return DrugAssociation.builder()
                .id(doc.getField(DrugField.DRUG_ID.name()).stringValue())
                .name(doc.getField(DrugField.DRUG_NAME.name()).stringValue())
                .url(String.format(DrugAssociation.URL_PATTERN, drugId))
                .geneId(doc.getField(DrugField.GENE_ID.name()).stringValue())
                .geneSymbol(doc.getField(DrugField.GENE_SYMBOL.name()).stringValue())
                .geneName(doc.getField(DrugField.GENE_NAME.name()).stringValue())
                .drugType(doc.getField(DrugField.DRUG_TYPE.name()).stringValue())
                .mechanismOfAction(doc.getField(DrugField.MECHANISM_OF_ACTION.name()).stringValue())
                .actionType(doc.getField(DrugField.ACTION_TYPE.name()).stringValue())
                .phase(doc.getField(DrugField.PHASE.name()).stringValue())
                .status(doc.getField(DrugField.STATUS.name()).stringValue())
                .source(source)
                .build();
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return DrugField.valueOf(fieldName).getType();
    }

    private DrugFieldValues getFieldValues(final Query query) throws IOException, ParseException {
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
        return DrugAssociation.builder()
                .id(jsonNodes.at("/drugId").asText())
                .name(jsonNodes.at("/prefName").asText())
                .geneId(jsonNodes.at("/targetId").asText())
                .disease(disease)
                .drugType(jsonNodes.at("/drugType").asText())
                .mechanismOfAction(jsonNodes.at("/mechanismOfAction").asText())
                .actionType(jsonNodes.at("/mechanismOfAction").asText())
                .phase(jsonNodes.at("/phase").asText())
                .status(jsonNodes.at("/status").asText())
                .source(source)
                .build();
    }

    private void fillGenes(final List<DrugAssociation> entries) throws ParseException, IOException {
        final List<String> geneIds = entries.stream()
                .map(DrugAssociation::getGeneId)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(geneIds)) {
            final List<TargetDetails> genes = targetDetailsManager.search(geneIds);
            final Map<String, TargetDetails> genesMap = genes.stream()
                    .collect(Collectors.toMap(TargetDetails::getId, Function.identity()));
            for (DrugAssociation r : entries) {
                if (genesMap.containsKey(r.getGeneId())) {
                    r.setGeneSymbol(genesMap.get(r.getGeneId()).getSymbol());
                    r.setGeneName(genesMap.get(r.getGeneId()).getName());
                }
            }
        }
    }

    private void fillDiseases(final List<DrugAssociation> entries) throws ParseException, IOException {
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
}
