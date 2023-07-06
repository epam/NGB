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
package com.epam.catgenome.manager.target;

import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.entity.externaldb.opentarget.AssociationType;
import com.epam.catgenome.entity.externaldb.opentarget.BareDisease;
import com.epam.catgenome.entity.externaldb.opentarget.Disease;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.opentarget.DrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.TargetDetails;
import com.epam.catgenome.entity.externaldb.opentarget.UrlEntity;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.manager.externaldb.AssociationSearchRequest;
import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.PudMedService;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugField;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugSearchRequest;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseManager;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseSearchRequest;
import com.epam.catgenome.manager.externaldb.opentarget.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.opentarget.DrugField;
import com.epam.catgenome.manager.externaldb.opentarget.DrugSearchRequest;
import com.epam.catgenome.manager.externaldb.opentarget.TargetDetailsManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDiseaseField;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDiseaseSearchRequest;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugField;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugSearchRequest;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBGeneManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.externaldb.opentarget.DiseaseManager.getDiseaseUrl;

@Service
@RequiredArgsConstructor
public class TargetIdentificationManager {

    private static final String PUBMED_LINK = "PubMed:<a href='https://europepmc.org/article/med/%s'>%s</a>";
    private static final String PUBMED_PATTERN = "PubMed:[0-9]+";
    private final TargetManager targetManager;
    private final PharmGKBGeneManager pharmGKBGeneManager;
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final NCBIGeneManager geneManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final TargetDetailsManager targetDetailsManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final DiseaseManager diseaseManager;
    private final PudMedService pudMedService;

    public IdentificationResult launchIdentification(final IdentificationRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {
        targetManager.getTarget(request.getTargetId());
        final List<String> geneIds = ListUtils.union(ListUtils.emptyIfNull(request.getTranslationalGenes()),
                ListUtils.emptyIfNull(request.getGenesOfInterest()));
        Assert.isTrue(!CollectionUtils.isEmpty(geneIds),
                "Either Species of interest or Translational species must me specified.");
        final Map<String, String> entrezMap = ncbiGeneIdsManager.searchByEnsemblIds(geneIds);
        final Map<String, String> description = getDescriptions(entrezMap);
        final long drugsCount = getDrugsCount(geneIds);
        final long diseasesCount = getDiseasesCount(geneIds);
        return IdentificationResult.builder()
                .description(description)
                .diseasesCount(diseasesCount)
                .knownDrugsCount(drugsCount)
                .build();
    }

    public SearchResult<DGIDBDrugAssociation> getDGIDbDrugs(final DGIDBDrugSearchRequest request)
            throws IOException, ParseException {
        return dgidbDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final PharmGKBDrugSearchRequest request)
            throws IOException, ParseException {
        return pharmGKBDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDisease> getPharmGKBDiseases(final PharmGKBDiseaseSearchRequest request)
            throws IOException, ParseException {
        return pharmGKBDiseaseAssociationManager.search(request);
    }

    public SearchResult<DrugAssociation> getOpenTargetsDrugs(final DrugSearchRequest request)
            throws IOException, ParseException {
        return drugAssociationManager.search(request);
    }

    public SearchResult<DiseaseAssociationAggregated> getOpenTargetsDiseases(final DiseaseSearchRequest request)
            throws IOException, ParseException {
        final SearchResult<DiseaseAssociation> result = diseaseAssociationManager.search(request);
        final List<DiseaseAssociationAggregated> converted = result.getItems().stream()
                .map(this::aggregate)
                .collect(Collectors.toList());
        final SearchResult<DiseaseAssociationAggregated> convertedResult = new SearchResult<>();
        convertedResult.setItems(converted);
        convertedResult.setTotalCount(result.getTotalCount());
        return convertedResult;
    }

    public List<DiseaseAssociationAggregated> getAllOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final List<DiseaseAssociation> result = diseaseAssociationManager.searchAll(request.getGeneIds());
        final List<DiseaseAssociationAggregated> converted = result.stream()
                .map(this::aggregate)
                .collect(Collectors.toList());
        fillTANames(converted);
        return converted;
    }

    public void importOpenTargetsData(final String targetsPath,
                                      final String diseasesPath,
                                      final String drugsPath,
                                      final String overallScoresPath,
                                      final String scoresPath) throws IOException, ParseException {
        targetDetailsManager.importData(targetsPath);
        diseaseManager.importData(diseasesPath);
        drugAssociationManager.importData(drugsPath);
        diseaseAssociationManager.importData(scoresPath, overallScoresPath);
    }

    public void importPharmGKBData(final String genePath, final String drugPath,
                                   final String drugAssociationPath, final String diseaseAssociationPath)
            throws IOException, ParseException {
        pharmGKBGeneManager.importData(genePath);
        pharmGKBDrugAssociationManager.importData(drugPath, drugAssociationPath);
        pharmGKBDiseaseAssociationManager.importData(diseaseAssociationPath);
    }

    public void importDGIdbData(final String path) throws IOException, ParseException {
        dgidbDrugAssociationManager.importData(path);
    }

    public List<String> getPharmGKBDrugFieldValues(final PharmGKBDrugField field) throws IOException {
        return pharmGKBDrugAssociationManager.getFieldValues(field);
    }

    public List<String> getPharmGKBDiseaseFieldValues(final PharmGKBDiseaseField field) throws IOException {
        return pharmGKBDiseaseAssociationManager.getFieldValues(field);
    }

    public List<String> getDGIDBDrugFieldValues(final DGIDBDrugField field) throws IOException {
        return dgidbDrugAssociationManager.getFieldValues(field);
    }

    public List<String> getDrugFieldValues(final DrugField field) throws IOException {
        return drugAssociationManager.getFieldValues(field);
    }

    public SearchResult<NCBISummaryVO> getPublications(final PublicationSearchRequest request) {
        return pudMedService.fetchPubMedArticles(request.getGeneIds());
    }

    private Map<String, String> getDescriptions(final Map<String, String> entrezMap)
            throws ExternalDbUnavailableException, ParseException, IOException {
        final Map<String, String> ncbiDescription = geneManager.fetchGeneSummaryByIds(entrezMap);
        final List<TargetDetails> openTargetDetails = targetDetailsManager.search(new ArrayList<>(entrezMap.values()));
        final Map<String, String> openTargetDescriptionMap = openTargetDetails.stream()
                .collect(Collectors.toMap(TargetDetails::getId, TargetDetails::getDescription));
        final Map<String, String> merged = mergeDescriptions(ncbiDescription, openTargetDescriptionMap);
        merged.replaceAll((key, value) -> setHyperLinks(value));
        return merged;
    }

    private String setHyperLinks(final String text) {
        String newText = text;
        final Pattern pattern = Pattern.compile(PUBMED_PATTERN);
        final Matcher matcher = pattern.matcher(text);
        final List<String> matches = new ArrayList<>();
        while(matcher.find()) {
            matches.add(matcher.group());
        }
        for (String match : matches) {
            String pubId = match.split(":")[1];
            String withLink = String.format(PUBMED_LINK, pubId, pubId);
            newText = newText.replace(match, withLink);
        }
        return newText;
    }

    private long getDrugsCount(final List<String> ensemblIds) throws IOException, ParseException {
        final long pharmGKBDrugsCount = pharmGKBDrugAssociationManager.totalCount(ensemblIds);
        final long dgidbDrugsCount = dgidbDrugAssociationManager.totalCount(ensemblIds);
        final long openTargetDrugs = drugAssociationManager.totalCount(ensemblIds);
        return pharmGKBDrugsCount + dgidbDrugsCount + openTargetDrugs;
    }

    private long getDiseasesCount(final List<String> geneIds) throws ParseException, IOException {
        final long openTargetsDiseasesCount = diseaseAssociationManager.totalCount(geneIds);
        final long pharmGKBDiseasesCount = pharmGKBDiseaseAssociationManager.totalCount(geneIds);
        return openTargetsDiseasesCount + pharmGKBDiseasesCount;
    }

    private static Map<String, String> mergeDescriptions(final Map<String, String> ncbiDescriptions,
                                                         final Map<String, String> openTargetDescriptions) {
        ncbiDescriptions.forEach((geneId, value) -> {
            if (openTargetDescriptions.containsKey(geneId)) {
                openTargetDescriptions.replace(geneId, openTargetDescriptions.get(geneId) + value);
            } else {
                openTargetDescriptions.put(geneId, value);
            }
        });
        return openTargetDescriptions;
    }

    private DiseaseAssociationAggregated aggregate(final DiseaseAssociation association) {
        final Disease disease = Disease.builder()
                .id(association.getDiseaseId())
                .name(association.getDiseaseName())
                .url(getDiseaseUrl(association.getDiseaseId()))
                .build();
        final Map<AssociationType, Float> scores = new HashMap<>();
        if (association.getOverallScore() != null) {
            scores.put(AssociationType.OVERALL, association.getOverallScore());
        }
        if (association.getGeneticAssociationScore() != null) {
            scores.put(AssociationType.GENETIC_ASSOCIATIONS, association.getGeneticAssociationScore());
        }
        if (association.getSomaticMutationScore() != null) {
            scores.put(AssociationType.SOMATIC_MUTATIONS, association.getSomaticMutationScore());
        }
        if (association.getKnownDrugScore() != null) {
            scores.put(AssociationType.DRUGS, association.getKnownDrugScore());
        }
        if (association.getAffectedPathwayScore() != null) {
            scores.put(AssociationType.PATHWAYS, association.getAffectedPathwayScore());
        }
        if (association.getLiteratureScore() != null) {
            scores.put(AssociationType.TEXT_MINING, association.getLiteratureScore());
        }
        if (association.getRnaExpressionScore() != null) {
            scores.put(AssociationType.RNA_EXPRESSION, association.getRnaExpressionScore());
        }
        if (association.getAnimalModelScore() != null) {
            scores.put(AssociationType.ANIMAL_MODELS, association.getAnimalModelScore());
        }
        return DiseaseAssociationAggregated.builder()
                .geneId(association.getGeneId())
                .disease(disease)
                .scores(scores)
                .build();
    }

    private void fillTANames(final List<DiseaseAssociationAggregated> records) throws ParseException, IOException {
        final Set<String> diseaseIds = records.stream().map(r -> r.getDisease().getId()).collect(Collectors.toSet());
        final Map<String, Disease> diseases = getDiseasesMap(diseaseIds);
        final Set<String> taIds = new HashSet<>();
        for (Disease d : diseases.values()) {
            List<UrlEntity> therapeuticAreas = d.getTherapeuticAreas();
            if (CollectionUtils.isNotEmpty(therapeuticAreas)) {
                taIds.addAll(therapeuticAreas.stream().map(UrlEntity::getId).collect(Collectors.toList()));
            }
        }
        if (CollectionUtils.isEmpty(taIds)) {
            return;
        }
        final Map<String, UrlEntity> tas = getTAsMap(taIds);
        for (DiseaseAssociationAggregated r : records) {
            String diseaseId = r.getDisease().getId();
            Disease disease = diseases.get(diseaseId);
            List<UrlEntity> diseaseTAs = disease.getTherapeuticAreas();
            List<UrlEntity> newDiseaseTAs = diseaseTAs.stream()
                    .map(ta -> tas.getOrDefault(ta.getId(), ta))
                    .collect(Collectors.toList());
            disease.setTherapeuticAreas(newDiseaseTAs);
            r.setDisease(disease);
        }
    }

    public List<BareDisease> getDiseasesTree() throws IOException {
        return diseaseManager.search();
    } 

    private Map<String, UrlEntity> getTAsMap(final Set<String> taIds) throws ParseException, IOException {
        final List<Disease> tas = diseaseManager.search(new ArrayList<>(taIds));
        return CollectionUtils.isEmpty(tas) ? Collections.emptyMap() : tas.stream()
                .map(t -> new UrlEntity(t.getId(), t.getName(), t.getUrl()))
                .collect(Collectors.toMap(UrlEntity::getId, Function.identity()));
    }

    private Map<String, Disease> getDiseasesMap(final Set<String> diseaseIds) throws ParseException, IOException {
        final List<Disease> diseases = diseaseManager.search(new ArrayList<>(diseaseIds));
        return CollectionUtils.isEmpty(diseases) ? Collections.emptyMap() : diseases.stream()
                .collect(Collectors.toMap(Disease::getId, Function.identity()));
    }
}
