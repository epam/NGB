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

import com.epam.catgenome.entity.externaldb.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.opentarget.*;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBDrugAssociation;
import com.epam.catgenome.entity.externaldb.pharmgkb.PharmGKBGene;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.IdentificationResult;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.opentarget.DiseaseManager;
import com.epam.catgenome.manager.externaldb.opentarget.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.opentarget.TargetDetailsManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBDrugManager;
import com.epam.catgenome.manager.externaldb.pharmgkb.PharmGKBGeneManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TargetIdentificationManager {

    private final TargetManager targetManager;
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDrugManager pharmGKBDrugManager;
    private final PharmGKBGeneManager pharmGKBGeneManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final NCBIGeneManager geneManager;
    private final TargetDetailsManager targetDetailsManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final DiseaseManager diseaseManager;

    public IdentificationResult launchIdentification(final IdentificationRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {
        targetManager.getTarget(request.getTargetId());
        final List<String> geneIds = ListUtils.union(ListUtils.emptyIfNull(request.getTranslationalGenes()),
                ListUtils.emptyIfNull(request.getGenesOfInterest()));
        Assert.isTrue(!CollectionUtils.isEmpty(geneIds),
                "Either Species of interest or Translational species must me specified.");
        final Map<String, String> entrezMap = geneManager.getEntrezMap(geneIds);
        final Map<String, String> description = getDescriptions(entrezMap);
        final long drugsCount = getDrugsCount(entrezMap);
        final int diseasesCount = diseaseAssociationManager.totalCount(geneIds);
        return IdentificationResult.builder()
                .description(description)
                .diseasesCount(diseasesCount)
                .knownDrugsCount(drugsCount)
                .build();
    }

    public SearchResult<DGIDBDrugAssociation> getDGIDbDrugs(final AssociationSearchRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {
        final Map<String, String> entrezMap = geneManager.getEntrezMap(request.getGeneIds());
        request.setGeneIds(new ArrayList<>(entrezMap.keySet()));
        return dgidbDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final AssociationSearchRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {
        final Map<String, String> entrezMap = geneManager.getEntrezMap(request.getGeneIds());
        final List<PharmGKBGene> pharmGKBGenes = pharmGKBGeneManager.search(new ArrayList<>(entrezMap.keySet()));
        final SearchResult<PharmGKBDrug> result = new SearchResult<>();
        if (CollectionUtils.isEmpty(pharmGKBGenes)) {
            return result;
        }
        final List<String> pharmGeneIds = pharmGKBGenes.stream()
                .map(PharmGKBGene::getPharmGKBId)
                .collect(Collectors.toList());
        request.setGeneIds(pharmGeneIds);
        final SearchResult<PharmGKBDrugAssociation> drugAssociations =
                pharmGKBDrugAssociationManager.search(request);
        final List<PharmGKBDrugAssociation> drugAssociationItems = drugAssociations.getItems();
        if (CollectionUtils.isEmpty(drugAssociationItems)) {
            return result;
        }
        final List<String> drugIds = drugAssociationItems.stream()
                .map(PharmGKBDrugAssociation::getDrugId)
                .collect(Collectors.toList());
        final List<PharmGKBDrug> drugs = pharmGKBDrugManager.search(drugIds);
        if (CollectionUtils.isEmpty(drugs)) {
            return result;
        }
        PharmGKBGene gene = null;
        for (PharmGKBDrug drug: drugs) {
            PharmGKBDrugAssociation item = drugAssociationItems.stream()
                    .filter(i -> i.getDrugId().equals(drug.getDrugId()))
                    .findAny()
                    .orElse(null);
            if (item != null) {
                gene = pharmGKBGenes.stream()
                        .filter(g -> g.getPharmGKBId().equals(item.getPharmGKBGeneId()))
                        .findAny()
                        .orElse(null);
            }
            if (gene != null) {
                drug.setGeneId(entrezMap.get(gene.getEntrezId()));
            }
        }
        result.setItems(drugs);
        result.setTotalCount(drugAssociations.getTotalCount());
        return result;
    }

    public SearchResult<DrugAssociation> getOpenTargetsDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final SearchResult<DrugAssociation> result = drugAssociationManager.search(request);
        fillDiseaseNamesForDrugs(result.getItems());
        return result;
    }

    public SearchResult<DiseaseAssociationAggregated> getOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final SearchResult<DiseaseAssociationAggregated> result = diseaseAssociationManager.search(request);
        fillDiseaseNames(result.getItems());
        return result;
    }

    public List<DiseaseAssociationAggregated> getAllOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        final List<DiseaseAssociationAggregated> result = diseaseAssociationManager.searchAll(request.getGeneIds());
        fillDiseaseNames(result);
        fillTANames(result);
        return result;
    }

    public void importOpenTargetsData(final String targetsPath, final String diseasesPath, final String drugsPath,
                                      final String overallScoresPath, final String scoresPath) throws IOException {
        if (!TextUtils.isBlank(targetsPath)) {
            targetDetailsManager.importData(targetsPath);
        }
        if (!TextUtils.isBlank(diseasesPath)) {
            diseaseManager.importData(diseasesPath);
        }
        if (!TextUtils.isBlank(drugsPath)) {
            drugAssociationManager.importData(drugsPath);
        }
        if (!TextUtils.isBlank(overallScoresPath) && !TextUtils.isBlank(scoresPath)) {
            diseaseAssociationManager.importData(scoresPath, overallScoresPath);
        }
    }

    public void importPharmGKBData(final String genePath, final String drugPath, final String drugAssociationPath)
            throws IOException {
        if (!TextUtils.isBlank(genePath)) {
            pharmGKBGeneManager.importData(genePath);
        }
        if (!TextUtils.isBlank(drugPath)) {
            pharmGKBDrugManager.importData(drugPath);
        }
        if (!TextUtils.isBlank(drugAssociationPath)) {
            pharmGKBDrugAssociationManager.importData(drugAssociationPath);
        }
    }

    public void importDGIdbData(final String path) throws IOException {
        dgidbDrugAssociationManager.importData(path);
    }

    private Map<String, String> getDescriptions(final Map<String, String> entrezMap)
            throws ExternalDbUnavailableException, ParseException, IOException {
        final Map<String, String> ncbiDescription = geneManager.fetchGeneSummaryByIds(entrezMap);
        final List<TargetDetails> openTargetDetails = targetDetailsManager.search(new ArrayList<>(entrezMap.values()));
        final Map<String, String> openTargetDescriptionMap = openTargetDetails.stream()
                .collect(Collectors.toMap(TargetDetails::getId, TargetDetails::getDescription));
        return mergeDescriptions(ncbiDescription, openTargetDescriptionMap);
    }

    private long getDrugsCount(final Map<String, String> entrezMap) throws IOException, ParseException {
        final List<String> entrezIds = new ArrayList<>(entrezMap.keySet());
        final List<String> ensemblIds = new ArrayList<>(entrezMap.values());
        final long gkbDrugsCount = getGkbDrugsCount(entrezIds);
        final long dgiDBDrugsCount = dgidbDrugAssociationManager.totalCount(entrezIds);
        final long openTargetDrugs = drugAssociationManager.totalCount(ensemblIds);
        return gkbDrugsCount + dgiDBDrugsCount + openTargetDrugs;
    }

    private Long getGkbDrugsCount(final List<String> entrezIds) throws IOException, ParseException {
        final List<PharmGKBGene> gkbGenes = pharmGKBGeneManager.search(entrezIds);
        final List<String> gkbGeneIds = gkbGenes.stream().map(PharmGKBGene::getPharmGKBId).collect(Collectors.toList());
        return pharmGKBDrugAssociationManager.totalCount(gkbGeneIds);
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

    private void fillDiseaseNames(final List<DiseaseAssociationAggregated> entries) throws ParseException, IOException {
        final List<String> diseaseIds = entries.stream()
                .map(r -> r.getDisease().getId())
                .distinct()
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(diseaseIds)) {
            final List<Disease> diseases = diseaseManager.search(diseaseIds);
            final Map<String, Disease> diseasesMap = diseases.stream()
                    .collect(Collectors.toMap(Disease::getId, Function.identity()));
            for (DiseaseAssociationAggregated r : entries) {
                if (diseasesMap.containsKey(r.getDisease().getId())) {
                    r.setDisease(diseasesMap.get(r.getDisease().getId()));
                }
            }
        }
    }

    private void fillDiseaseNamesForDrugs(final List<DrugAssociation> entries) throws ParseException, IOException {
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

    private void fillTANames(final List<DiseaseAssociationAggregated> records) throws ParseException, IOException {
        final Set<String> therapeuticAreaIds = new HashSet<>();
        for (DiseaseAssociationAggregated r : records) {
            List<UrlEntity> therapeuticAreas = r.getDisease().getTherapeuticAreas();
            if (CollectionUtils.isNotEmpty(therapeuticAreas)) {
                therapeuticAreaIds.addAll(therapeuticAreas.stream().map(UrlEntity::getId).collect(Collectors.toList()));
            }
        }
        if (CollectionUtils.isNotEmpty(therapeuticAreaIds)) {
            final List<Disease> diseases = diseaseManager.search(new ArrayList<>(therapeuticAreaIds));
            if (CollectionUtils.isEmpty(diseases)) {
                return;
            }
            final Map<String, UrlEntity> diseasesMap = diseases.stream()
                    .map(t -> new UrlEntity(t.getId(), t.getName(), t.getUrl()))
                    .collect(Collectors.toMap(UrlEntity::getId, Function.identity()));
            for (DiseaseAssociationAggregated r : records) {
                Disease disease = r.getDisease();
                List<UrlEntity> diseaseTAs = disease.getTherapeuticAreas();
                if (CollectionUtils.isNotEmpty(diseaseTAs)) {
                    List<UrlEntity> newDiseaseTAs = new ArrayList<>();
                    for (UrlEntity diseaseTA : diseaseTAs) {
                        newDiseaseTAs.add(diseasesMap.getOrDefault(diseaseTA.getId(), diseaseTA));
                    }
                    disease.setTherapeuticAreas(newDiseaseTAs);
                }
            }
        }
    }
}
