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
import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.target.opentargets.*;
import com.epam.catgenome.entity.index.FilterType;
import com.epam.catgenome.manager.externaldb.OpenTargetsManager;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.target.AbstractAssociationManager;
import com.epam.catgenome.manager.index.OrderInfo;
import com.epam.catgenome.manager.index.SearchRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatDocValuesField;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.SortedDocValuesField;
import org.apache.lucene.document.StoredField;
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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.util.NgbFileUtils.getDirectory;

@Service
public class DiseaseAssociationManager extends AbstractAssociationManager<DiseaseAssociation> {

    @Value("${targets.opentargets.overallScoresDir:associationByOverallDirect}")
    private String overallScoresDir;
    @Value("${targets.opentargets.scoresDir:associationByDatatypeDirect}")
    private String scoresDir;
    private final DiseaseManager diseaseManager;
    private final OpenTargetsManager openTargetsManager;
    private final TargetDetailsManager targetDetailsManager;

    public DiseaseAssociationManager(final @Value("${targets.index.directory}") String indexDirectory,
                                     final @Value("${targets.top.hits:10000}") int targetsTopHits,
                                     final DiseaseManager diseaseManager,
                                     final TargetDetailsManager targetDetailsManager,
                                     final OpenTargetsManager openTargetsManager) {
        super(Paths.get(indexDirectory, "opentargets.disease.association").toString(), targetsTopHits);
        this.diseaseManager = diseaseManager;
        this.targetDetailsManager = targetDetailsManager;
        this.openTargetsManager = openTargetsManager;
    }


    private List<DiseaseAssociation> searchAll(final Query query)
            throws IOException {
        final List<DiseaseAssociation> entries = new ArrayList<>();
        try (Directory index = new SimpleFSDirectory(Paths.get(indexDirectory));
             IndexReader indexReader = DirectoryReader.open(index)) {
            IndexSearcher searcher = new IndexSearcher(indexReader);
            TopDocs topDocs = searcher.search(query, topHits);
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

    public long totalCount(final String diseaseId) throws ParseException, IOException {
        final List<DiseaseAssociation> result = search(diseaseId);
        return result.stream().map(DiseaseAssociation::getGeneId).distinct().count();
    }

    public SearchResult<DiseaseAssociation> search(final SearchRequest request, final String diseaseId)
            throws ParseException, IOException {
        final SearchResult<DiseaseAssociation> result = super.search(request, diseaseId);
        fillHomologues(result);
        return result;
    }

    @Override
    public List<DiseaseAssociation> processEntries(final List<DiseaseAssociation> entries)
            throws ParseException, IOException {
        final List<DiseaseAssociation> processedEntries = convertEntries(entries);
        fillDiseases(processedEntries);
        fillGenes(processedEntries);
        return processedEntries;
    }

    @Override
    public List<DiseaseAssociation> readEntries(final String path) throws IOException {
        final Path overallScoresPath = Paths.get(path, overallScoresDir);
        final Path scoresPath = Paths.get(path, scoresDir);
        final File directory = getDirectory(scoresPath.toString());
        final File overallDirectory = getDirectory(overallScoresPath.toString());
        final List<DiseaseAssociation> entries = new ArrayList<>();
        String line;
        final ObjectMapper objectMapper = new ObjectMapper();
        for (File f: ArrayUtils.addAll(directory.listFiles(), overallDirectory.listFiles())) {
            try (Reader reader = new FileReader(f); BufferedReader bufferedReader = new BufferedReader(reader)) {
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
        }
        return entries;
    }

    @Override
    public SortField getDefaultSortField() {
        return new SortField(DiseaseField.OVERALL_SCORE.name(), SortField.Type.FLOAT, true);
    }

    @Override
    public void addDoc(final IndexWriter writer, final DiseaseAssociation entry) throws IOException {
        final Document doc = new Document();
        doc.add(new TextField(DiseaseField.GENE_ID.name(), entry.getGeneId(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DiseaseField.GENE_ID.name(), new BytesRef(entry.getGeneId())));

        final String geneSymbol = Optional.ofNullable(entry.getGeneSymbol()).orElse("");
        doc.add(new TextField(DiseaseField.GENE_SYMBOL.name(), geneSymbol, Field.Store.YES));
        doc.add(new SortedDocValuesField(DiseaseField.GENE_SYMBOL.name(), new BytesRef(geneSymbol)));

        final String geneName = Optional.ofNullable(entry.getGeneName()).orElse("");
        doc.add(new TextField(DiseaseField.GENE_NAME.name(), geneName, Field.Store.YES));
        doc.add(new SortedDocValuesField(DiseaseField.GENE_NAME.name(), new BytesRef(geneName)));

        doc.add(new TextField(DiseaseField.DISEASE_ID.name(), entry.getId(), Field.Store.YES));

        doc.add(new TextField(DiseaseField.DISEASE_NAME.name(), entry.getName(), Field.Store.YES));
        doc.add(new SortedDocValuesField(DiseaseField.DISEASE_NAME.name(), new BytesRef(entry.getName())));

        addFloatField(entry.getOverallScore(), doc, DiseaseField.OVERALL_SCORE);
        addFloatField(entry.getGeneticAssociationScore(), doc, DiseaseField.GENETIC_ASSOCIATIONS_SCORE);
        addFloatField(entry.getSomaticMutationScore(), doc, DiseaseField.SOMATIC_MUTATIONS_SCORE);
        addFloatField(entry.getKnownDrugScore(), doc, DiseaseField.DRUGS_SCORE);
        addFloatField(entry.getAffectedPathwayScore(), doc, DiseaseField.PATHWAYS_SCORE);
        addFloatField(entry.getLiteratureScore(), doc, DiseaseField.TEXT_MINING_SCORE);
        addFloatField(entry.getRnaExpressionScore(), doc, DiseaseField.RNA_EXPRESSION_SCORE);
        addFloatField(entry.getAnimalModelScore(), doc, DiseaseField.ANIMAL_MODELS_SCORE);
        writer.addDocument(doc);
    }

    @Override
    public DiseaseAssociation entryFromDoc(final Document doc) {
        final String id = doc.getField(DrugField.DISEASE_ID.name()).stringValue();
        return DiseaseAssociation.builder()
                .id(id)
                .name(doc.getField(DiseaseField.DISEASE_NAME.name()).stringValue())
                .url(String.format(Disease.URL_PATTERN, id))
                .geneId(doc.getField(DiseaseField.GENE_ID.name()).stringValue())
                .overallScore(getScore(doc, DiseaseField.OVERALL_SCORE))
                .geneticAssociationScore(getScore(doc, DiseaseField.GENETIC_ASSOCIATIONS_SCORE))
                .somaticMutationScore(getScore(doc, DiseaseField.SOMATIC_MUTATIONS_SCORE))
                .knownDrugScore(getScore(doc, DiseaseField.DRUGS_SCORE))
                .affectedPathwayScore(getScore(doc, DiseaseField.PATHWAYS_SCORE))
                .literatureScore(getScore(doc, DiseaseField.TEXT_MINING_SCORE))
                .rnaExpressionScore(getScore(doc, DiseaseField.RNA_EXPRESSION_SCORE))
                .animalModelScore(getScore(doc, DiseaseField.ANIMAL_MODELS_SCORE))
                .build();
    }

    @Override
    public DiseaseAssociation entryFromDocDiseaseView(final Document doc) {
        final String id = doc.getField(DrugField.DISEASE_ID.name()).stringValue();
        return DiseaseAssociation.builder()
                .id(id)
                .geneId(doc.getField(DiseaseField.GENE_ID.name()).stringValue())
                .url(String.format(Disease.URL_PATTERN, id))
                .geneSymbol(doc.getField(DiseaseField.GENE_SYMBOL.name()).stringValue())
                .geneName(doc.getField(DiseaseField.GENE_NAME.name()).stringValue())
                .overallScore(getScore(doc, DiseaseField.OVERALL_SCORE))
                .geneticAssociationScore(getScore(doc, DiseaseField.GENETIC_ASSOCIATIONS_SCORE))
                .somaticMutationScore(getScore(doc, DiseaseField.SOMATIC_MUTATIONS_SCORE))
                .knownDrugScore(getScore(doc, DiseaseField.DRUGS_SCORE))
                .affectedPathwayScore(getScore(doc, DiseaseField.PATHWAYS_SCORE))
                .literatureScore(getScore(doc, DiseaseField.TEXT_MINING_SCORE))
                .rnaExpressionScore(getScore(doc, DiseaseField.RNA_EXPRESSION_SCORE))
                .animalModelScore(getScore(doc, DiseaseField.ANIMAL_MODELS_SCORE))
                .build();
    }

    @Override
    public Sort getSort(final List<OrderInfo> orderInfos) {
        final List<SortField> sortFields = new ArrayList<>();
        if (orderInfos == null) {
            sortFields.add(getDefaultSortField());
        } else {
            for (OrderInfo orderInfo : orderInfos) {
                final SortField.Type sortType = orderInfo.getOrderBy().equals(DiseaseField.DISEASE_NAME.name())
                        || orderInfo.getOrderBy().equals(DiseaseField.GENE_ID.name())
                        || orderInfo.getOrderBy().equals(DiseaseField.GENE_SYMBOL.name())
                        || orderInfo.getOrderBy().equals(DiseaseField.GENE_NAME.name()) ?
                        SortField.Type.STRING : SortField.Type.FLOAT;
                final SortField sortField = new SortField(orderInfo.getOrderBy(),
                        sortType, orderInfo.isReverse());
                sortFields.add(sortField);
            }
        }
        return new Sort(sortFields.toArray(new SortField[sortFields.size()]));
    }

    @Override
    public FilterType getFilterType(String fieldName) {
        return DiseaseField.valueOf(fieldName).getType();
    }

    private static DiseaseAssociation entryFromJson(final JsonNode jsonNodes) {
        return DiseaseAssociation.builder()
                .geneId(jsonNodes.at("/targetId").asText())
                .id(jsonNodes.at("/diseaseId").asText())
                .type(jsonNodes.has("datatypeId") ?
                        AssociationType.getByName(jsonNodes.at("/datatypeId").asText()) :
                        AssociationType.OVERALL)
                .score(Float.parseFloat(jsonNodes.at("/score").asText()))
                .build();
    }

    private List<DiseaseAssociation> convertEntries(final List<DiseaseAssociation> entries) {
        final List<DiseaseAssociation> results = new ArrayList<>();
        final Map<AbstractMap.SimpleEntry<String, String>, List<DiseaseAssociation>> grouped = entries.stream()
                .collect(Collectors.groupingBy(entry ->
                        new AbstractMap.SimpleEntry<>(entry.getGeneId(), entry.getId())));
        for (Map.Entry<AbstractMap.SimpleEntry<String, String>, List<DiseaseAssociation>> group : grouped.entrySet()) {
            DiseaseAssociation result = new DiseaseAssociation();
            AbstractMap.SimpleEntry<String, String> key = group.getKey();
            result.setGeneId(key.getKey());
            result.setId(key.getValue());
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

    private void fillDiseases(final List<DiseaseAssociation> entries) throws ParseException, IOException {
        final List<String> diseaseIds = entries.stream()
                .map(DiseaseAssociation::getId)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(diseaseIds)) {
            final List<Disease> diseases = diseaseManager.search(diseaseIds);
            final Map<String, Disease> diseasesMap = diseases.stream()
                    .collect(Collectors.toMap(Disease::getId, Function.identity()));
            for (DiseaseAssociation r : entries) {
                if (diseasesMap.containsKey(r.getId())) {
                    r.setName(diseasesMap.get(r.getId()).getName());
                }
            }
        }
    }

    private void fillGenes(final List<DiseaseAssociation> entries) throws ParseException, IOException {
        final List<String> geneIds = entries.stream()
                .map(DiseaseAssociation::getGeneId)
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(geneIds)) {
            final List<TargetDetails> genes = targetDetailsManager.search(geneIds);
            final Map<String, TargetDetails> genesMap = genes.stream()
                    .collect(Collectors.toMap(TargetDetails::getId, Function.identity()));
            for (DiseaseAssociation r : entries) {
                if (genesMap.containsKey(r.getGeneId())) {
                    r.setGeneSymbol(genesMap.get(r.getGeneId()).getSymbol());
                    r.setGeneName(genesMap.get(r.getGeneId()).getName());
                }
            }
        }
    }

    private static void addFloatField(final Float entry, final Document doc, final DiseaseField field) {
        if (entry != null) {
            doc.add(new FloatPoint(field.name(), entry));
            doc.add(new StoredField(field.name(), entry));
            doc.add(new FloatDocValuesField(field.name(), entry));
        }
    }

    private static Float getScore(final Document doc, final DiseaseField field) {
        return doc.getField(field.name()) != null ? doc.getField(field.name()).numericValue().floatValue() : null;
    }

    private void fillHomologues(final SearchResult<DiseaseAssociation> result) {
        final List<String> targetIds = result.getItems().stream()
                .map(DiseaseAssociation::getGeneId)
                .collect(Collectors.toList());
        final Map<String, List<HomologGroup>> homologuesMap = openTargetsManager.getHomologues(targetIds);
        result.getItems().forEach(i -> {
            i.setHomologues(homologuesMap.get(i.getGeneId()));
        });
    }
}
