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
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.ncbi.GeneInfo;
import com.epam.catgenome.entity.externaldb.target.opentargets.AssociationType;
import com.epam.catgenome.entity.externaldb.target.opentargets.BareDisease;
import com.epam.catgenome.entity.externaldb.target.opentargets.Disease;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociationAggregated;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.TargetDetails;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.entity.target.GeneSequences;
import com.epam.catgenome.entity.target.IdentificationRequest;
import com.epam.catgenome.entity.target.TargetIdentificationResult;
import com.epam.catgenome.entity.target.SequencesSummary;
import com.epam.catgenome.manager.externaldb.PubMedService;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.taxonomy.Taxonomy;
import com.epam.catgenome.manager.externaldb.taxonomy.TaxonomyManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneInfoManager;
import com.epam.catgenome.manager.externaldb.sequence.NCBISequenceManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbEntriesManager;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.controller.vo.target.StructuresSearchRequest;
import com.epam.catgenome.manager.externaldb.target.AssociationSearchRequest;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugFieldValues;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.opentargets.TargetDetailsManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBGeneManager;
import com.epam.catgenome.manager.pdb.PdbFileManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class TargetIdentificationManager {

    private static final String PUBMED_LINK = "PubMed:<a href='https://europepmc.org/article/med/%s'>%s</a>";
    private static final String PUBMED_PATTERN = "PubMed:[0-9]+";
    private final TargetManager targetManager;
    private final PharmGKBGeneManager pharmGKBGeneManager;
    private final PharmGKBDrugManager pharmGKBDrugManager;
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final NCBIGeneManager geneManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final TargetDetailsManager targetDetailsManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final DiseaseManager diseaseManager;
    private final PubMedService pubMedService;
    private final NCBISequenceManager geneSequencesManager;
    private final PdbEntriesManager pdbEntriesManager;
    private final PdbFileManager pdbFileManager;
    private final NCBIGeneInfoManager geneInfoManager;
    private final TaxonomyManager taxonomyManager;

    public TargetIdentificationResult launchIdentification(final IdentificationRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {
        targetManager.getTarget(request.getTargetId());
        final List<String> geneIds = ListUtils.union(ListUtils.emptyIfNull(request.getTranslationalGenes()),
                ListUtils.emptyIfNull(request.getGenesOfInterest()));
        Assert.isTrue(!CollectionUtils.isEmpty(geneIds),
                "Either Species of interest or Translational species must me specified.");

        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        final List<String> entrezGeneIds = ncbiGeneIds.stream()
                .map(g -> g.getEntrezId().toString())
                .collect(Collectors.toList());
        final Map<String, String> description = getDescriptions(ncbiGeneIds);
        final Pair<Long, Long> drugsCount = getDrugsCount(geneIds);
        final long diseasesCount = getDiseasesCount(geneIds);
        final long publicationsCount = pubMedService.getPublicationsCount(entrezGeneIds);
        final SequencesSummary sequencesCount = geneSequencesManager.getSequencesCount(ncbiGeneIds);
        final long structuresCount = getStructuresCount(geneIds);
        return TargetIdentificationResult.builder()
                .description(description)
                .diseasesCount(diseasesCount)
                .knownDrugsRecordsCount(drugsCount.getLeft())
                .knownDrugsCount(drugsCount.getRight())
                .publicationsCount(publicationsCount)
                .sequencesCount(sequencesCount)
                .structuresCount(structuresCount)
                .build();
    }

    public SearchResult<DGIDBDrugAssociation> getDGIDbDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return dgidbDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return pharmGKBDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDisease> getPharmGKBDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return pharmGKBDiseaseAssociationManager.search(request);
    }

    public SearchResult<DrugAssociation> getOpenTargetsDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        return drugAssociationManager.search(request);
    }

    public SearchResult<DiseaseAssociationAggregated> getOpenTargetsDiseases(final AssociationSearchRequest request)
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

    public void importOpenTargetsData(final String path) throws IOException, ParseException {
        targetDetailsManager.importData(path);
        diseaseManager.importData(path);
        drugAssociationManager.importData(path);
        diseaseAssociationManager.importData(path);
    }

    public void importPharmGKBData(final String genePath, final String drugPath,
                                   final String drugAssociationPath, final String diseaseAssociationPath)
            throws IOException, ParseException {
        pharmGKBGeneManager.importData(genePath);
        pharmGKBDrugManager.importData(drugPath);
        pharmGKBDrugAssociationManager.importData(drugAssociationPath);
        pharmGKBDiseaseAssociationManager.importData(diseaseAssociationPath);
    }

    public void importDGIdbData(final String path) throws IOException, ParseException {
        dgidbDrugAssociationManager.importData(path);
    }

    public PharmGKBDrugFieldValues getPharmGKBDrugFieldValues(final List<String> geneIds)
            throws IOException, ParseException {
        return pharmGKBDrugAssociationManager.getFieldValues(geneIds);
    }

    public DGIDBDrugFieldValues getDGIDBDrugFieldValues(final List<String> geneIds)
            throws IOException, ParseException {
        return dgidbDrugAssociationManager.getFieldValues(geneIds);
    }

    public DrugFieldValues getDrugFieldValues(final List<String> geneIds) throws IOException, ParseException {
        return drugAssociationManager.getFieldValues(geneIds);
    }

    public List<BareDisease> getDiseasesTree() throws IOException {
        return diseaseManager.search();
    }

    public SearchResult<NCBISummaryVO> getPublications(final PublicationSearchRequest request) {
        return pubMedService.fetchPubMedArticles(request);
    }

    public String getArticlesAbstracts(final PublicationSearchRequest request) {
        return pubMedService.getArticleAbstracts(request);
    }

    public List<GeneSequences> getGeneSequences(final List<String> geneIds) throws ParseException, IOException {
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        if (CollectionUtils.isEmpty(ncbiGeneIds)) {
            return Collections.emptyList();
        }
        final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        return geneSequencesManager.fetchGeneSequences(entrezMap);
    }

    public List<GeneRefSection> getGeneSequencesTable(final List<String> geneIds, final Boolean getComments)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        if (CollectionUtils.isEmpty(ncbiGeneIds)) {
            return Collections.emptyList();
        }
        final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        return geneSequencesManager.getGeneSequencesTable(entrezMap, getComments);
    }

    public SearchResult<Structure> getStructures(final StructuresSearchRequest request) {
        final List<String> geneNames = targetManager.getTargetGeneNames(request.getGeneIds());
        return pdbEntriesManager.getStructures(request, geneNames);
    }

    public Map<String, String> getDescriptions(final List<GeneId> ncbiGeneIds)
            throws ExternalDbUnavailableException, ParseException, IOException {
        final List<String> geneIds = ncbiGeneIds.stream().map(GeneId::getEnsemblId).collect(Collectors.toList());
        final Map<GeneId, String> ncbiSummary = geneManager.fetchGeneSummaryByIds(ncbiGeneIds);
        final List<TargetDetails> openTargetDetails = targetDetailsManager.search(geneIds);
        final Map<String, String> merged = mergeDescriptions(ncbiSummary, openTargetDetails);
        merged.replaceAll((key, value) -> setHyperLinks(value));
        return merged;
    }

    public List<GeneInfo> getGenes(final String prefix) throws ParseException, IOException {
        final List<GeneInfo> genes = geneInfoManager.searchBySymbol(prefix);
        final List<Long> taxIds = genes.stream().map(GeneInfo::getTaxId).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(taxIds)) {
            final List<Taxonomy> species = taxonomyManager.searchOrganismsByIds(new HashSet<>(taxIds));
            final Map<Long, Taxonomy> speciesMap = species.stream()
                    .collect(Collectors.toMap(Taxonomy::getTaxId, Function.identity()));
            genes.forEach(g -> {
                if (speciesMap.containsKey(g.getTaxId())) {
                    g.setSpeciesScientificName(speciesMap.get(g.getTaxId()).getScientificName());
                    g.setSpeciesCommonName(speciesMap.get(g.getTaxId()).getCommonName());
                }
            });
        }
        return genes;
    }

    private long getStructuresCount(final List<String> geneIds) {
        long localPdbFiles = pdbFileManager.totalCount(geneIds);
        final List<String> geneNames = targetManager.getTargetGeneNames(geneIds);
        long structuresCount = pdbEntriesManager.getStructuresCount(geneNames);
        return localPdbFiles + structuresCount;
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

    private Pair<Long, Long> getDrugsCount(final List<String> geneIds) throws IOException, ParseException {
        final Pair<Long, Long> pharmGKBDrugsCount = pharmGKBDrugAssociationManager.recordsCount(geneIds);
        final Pair<Long, Long> dgidbDrugsCount = dgidbDrugAssociationManager.recordsCount(geneIds);
        final Pair<Long, Long> openTargetDrugs = drugAssociationManager.recordsCount(geneIds);
        return Pair.of(pharmGKBDrugsCount.getLeft() + dgidbDrugsCount.getLeft() + openTargetDrugs.getLeft(),
                pharmGKBDrugsCount.getRight() + dgidbDrugsCount.getRight() + openTargetDrugs.getRight());
    }

    private long getDiseasesCount(final List<String> geneIds) throws ParseException, IOException {
        final long openTargetsDiseasesCount = diseaseAssociationManager.totalCount(geneIds);
        final long pharmGKBDiseasesCount = pharmGKBDiseaseAssociationManager.totalCount(geneIds);
        return openTargetsDiseasesCount + pharmGKBDiseasesCount;
    }

    private static Map<String, String> mergeDescriptions(final Map<GeneId, String> ncbiSummary,
                                                         final List<TargetDetails> openTargetDetails) {
        final Map<String, String> ncbiDescriptions = new HashMap<>();
        ncbiSummary.forEach((key1, value1) -> ncbiDescriptions.put(key1.getEnsemblId().toLowerCase(), value1));

        final Map<String, String> openTargetDescriptionMap = openTargetDetails.stream()
                .collect(Collectors.toMap(t -> t.getId().toLowerCase(), TargetDetails::getDescription));

        ncbiDescriptions.forEach((geneId, value) -> {
            if (openTargetDescriptionMap.containsKey(geneId)) {
                openTargetDescriptionMap.replace(geneId, openTargetDescriptionMap.get(geneId) + value);
            } else {
                openTargetDescriptionMap.put(geneId, value);
            }
        });
        return openTargetDescriptionMap;
    }

    private DiseaseAssociationAggregated aggregate(final DiseaseAssociation association) {
        final Disease disease = Disease.builder()
                .id(association.getId())
                .name(association.getName())
                .url(String.format(Disease.URL_PATTERN, association.getId()))
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
