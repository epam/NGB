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
import com.epam.catgenome.entity.externaldb.target.DrugsCount;
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
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.target.*;
import com.epam.catgenome.manager.externaldb.PubMedService;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIEnsemblIdsManager;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDiseaseFieldValues;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDrugFieldValues;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDatabaseManager;
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
import com.epam.catgenome.manager.sequence.SequencesManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.CharEncoding.UTF_8;


@Service
@RequiredArgsConstructor
@Slf4j
public class LaunchIdentificationManager {

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
    private final NCBIEnsemblIdsManager ncbiEnsemblIdsManager;
    private final SequencesManager sequencesManager;
    private final TTDDatabaseManager ttdDatabaseManager;

    @Value("${targets.parasites.include.additional:false}")
    private boolean includeAdditionalGenes;

    @Value("${targets.parasites.include.additional.sequences:true}")
    private boolean includeAdditionalGenesSequences;


    public TargetIdentificationResult launchIdentification(final IdentificationRequest request)
            throws ExternalDbUnavailableException, IOException, ParseException {

        final List<String> geneIds = getGeneIds(request.getGenesOfInterest(), request.getTranslationalGenes());
        final List<String> expandedGeneIds = getExpandedGeneIds(request.getTargetId(), geneIds, includeAdditionalGenes);
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(expandedGeneIds);

        final Map<String, String> description = getDescriptions(ncbiGeneIds);
        final DrugsCount drugsCount = getDrugsCount(request.getTargetId(), expandedGeneIds);
        final long diseasesCount = getDiseasesCount(request.getTargetId(), expandedGeneIds);
        final long publicationsCount = getPublicationsCount(request.getTargetId(), geneIds, ncbiGeneIds);
        final SequencesSummary sequencesCount = getGeneSequencesCount(request.getTargetId(), geneIds);
        final long structuresCount = getStructuresCount(request.getTargetId(), expandedGeneIds);
        return TargetIdentificationResult.builder()
                .description(description)
                .diseasesCount(diseasesCount)
                .knownDrugsRecordsCount(drugsCount.getTotalCount())
                .knownDrugsCount(drugsCount.getDistinctCount())
                .publicationsCount(publicationsCount)
                .sequencesCount(sequencesCount)
                .structuresCount(structuresCount)
                .build();
    }

    public List<String> getExpandedGeneIds(final Long targetId,
                                           final List<String> geneIds,
                                           final boolean includeAdditionalGenes) throws ParseException, IOException {
        final Map<String, Long> targetGenes = targetManager.getTargetGenes(targetId, geneIds, includeAdditionalGenes);
        final Set<String> result = targetGenes.keySet().stream().map(String::toLowerCase).collect(Collectors.toSet());
        result.addAll(geneIds);
        return new ArrayList<>(result);
    }

    public static List<String> getGeneIds(final List<String> genesOfInterest, final List<String> translationalGenes) {
        final List<String> geneIds = Stream.concat(genesOfInterest.stream(),
                        Optional.ofNullable(translationalGenes).orElse(Collections.emptyList()).stream())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
        Assert.isTrue(!CollectionUtils.isEmpty(geneIds),
                "Either Species of interest or Translational species must me specified.");
        return geneIds;
    }

    public SearchResult<DGIDBDrugAssociation> getDGIDbDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        return dgidbDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDrug> getPharmGKBDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        return pharmGKBDrugAssociationManager.search(request);
    }

    public SearchResult<PharmGKBDisease> getPharmGKBDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        return pharmGKBDiseaseAssociationManager.search(request);
    }

    public SearchResult<DrugAssociation> getOpenTargetsDrugs(final AssociationSearchRequest request)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        return drugAssociationManager.search(request);
    }

    public SearchResult<DiseaseAssociationAggregated> getOpenTargetsDiseases(final AssociationSearchRequest request)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
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
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
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

    public PharmGKBDrugFieldValues getPharmGKBDrugFieldValues(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(targetId, geneIds);
        return pharmGKBDrugAssociationManager.getFieldValues(geneIds);
    }

    public DGIDBDrugFieldValues getDGIDBDrugFieldValues(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(targetId, geneIds);
        return dgidbDrugAssociationManager.getFieldValues(geneIds);
    }

    public DrugFieldValues getDrugFieldValues(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        targetManager.expandTargetGenes(targetId, geneIds);
        return drugAssociationManager.getFieldValues(geneIds);
    }

    public List<BareDisease> getDiseasesTree() throws IOException {
        return diseaseManager.search();
    }

    public SearchResult<NCBISummaryVO> getPublications(final PublicationSearchRequest request)
            throws ParseException, IOException, ExternalDbUnavailableException {
        if (request.getTargetId() != null) {
            final Target target = targetManager.getTarget(request.getTargetId());
            if (TargetType.PARASITE.equals(target.getType())) {
                return pubMedService.fetchPubMedArticles(request,
                        getPublicationsQuery(request.getTargetId(), request.getGeneIds()));
            }
        }
        final List<String> expandedGeneIds = getExpandedGeneIds(request.getTargetId(), request.getGeneIds(), true);
        request.setGeneIds(expandedGeneIds);
        return pubMedService.fetchPubMedArticles(request);
    }

    public String getArticlesAbstracts(final PublicationSearchRequest request) throws ParseException, IOException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        return pubMedService.getArticleAbstracts(request);
    }

    public List<GeneSequences> getGeneSequences(final Long targetId,
                                                final List<String> geneIds,
                                                final boolean includeAdditionalGenes)
            throws ParseException, IOException {
        if (includeAdditionalGenes) {
            targetManager.expandTargetGenes(targetId, geneIds);
        }
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        if (CollectionUtils.isEmpty(ncbiGeneIds)) {
            return Collections.emptyList();
        }
        final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
        return geneSequencesManager.fetchGeneSequences(entrezMap);
    }

    public List<GeneRefSection> getGeneSequencesTable(final Long targetId,
                                                      final List<String> geneIds,
                                                      final Boolean getComments,
                                                      final Boolean includeLocal,
                                                      final Boolean includeAdditionalGenes)
            throws ParseException, IOException, ExternalDbUnavailableException {

        List<GeneRefSection> result = new ArrayList<>();
        for (String geneId: geneIds) {
            final Map<String, Long> targetGenes = targetManager.getTargetGenes(targetId,
                    Collections.singletonList(geneId), includeAdditionalGenes);

            if (includeLocal) {
                result.addAll(sequencesManager.getGeneSequencesTable(geneId, targetGenes));
            }

            final List<String> expandedGeneIds = getExpandedGeneIds(targetId, Collections.singletonList(geneId),
                    includeAdditionalGenes);

            final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(expandedGeneIds);
            if (CollectionUtils.isNotEmpty(ncbiGeneIds)) {
                final Map<String, GeneId> entrezMap = ncbiGeneIds.stream()
                        .collect(Collectors.toMap(i -> i.getEntrezId().toString(), Function.identity()));
                result.addAll(geneSequencesManager.getGeneSequencesTable(geneId, entrezMap, getComments));
            }
        }
        return result;
    }

    public SequencesSummary getGeneSequencesCount(final Long targetId,
                                                  final List<String> geneIds)
            throws IOException, ExternalDbUnavailableException, ParseException {
        final List<TargetGene> targetGenes = ListUtils.emptyIfNull(targetManager.getTargetGenes(targetId, geneIds));
        if (targetGenes.stream().allMatch(g -> TargetGeneStatus.PROCESSED.equals(g.getStatus()))) {
            return targetGenes.stream().map(TargetGene::getSequencesSummary)
                    .filter(Objects::nonNull).reduce(new SequencesSummary(), SequencesSummary::sum);
        }
        final List<GeneRefSection> geneRefSections = getGeneSequencesTable(targetId, geneIds,
                false, true, includeAdditionalGenesSequences);
        return getSequencesSummary(geneRefSections);
    }

    public static SequencesSummary getSequencesSummary(final List<GeneRefSection> geneRefSections) {
        final SequencesSummary result = SequencesSummary.builder().build();
        result.setDNAs(geneRefSections.stream()
                .map(GeneRefSection::getReference)
                .filter(Objects::nonNull)
                .count());
        result.setMRNAs(geneRefSections.stream()
                .map(r -> Optional.ofNullable(r.getSequences()).orElse(Collections.emptyList()))
                .flatMap(List::stream)
                .map(GeneSequence::getMRNA).filter(Objects::nonNull).count());
        result.setProteins(geneRefSections.stream()
                .map(r -> Optional.ofNullable(r.getSequences()).orElse(Collections.emptyList()))
                .flatMap(List::stream)
                .map(GeneSequence::getProtein).filter(Objects::nonNull).count());
        return result;
    }

    public SearchResult<Structure> getStructures(final StructuresSearchRequest request)
            throws ParseException, IOException {
        targetManager.expandTargetGenes(request.getTargetId(), request.getGeneIds());
        final List<String> geneNames = getGeneNames(request.getTargetId(), request.getGeneIds());
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

    public List<GeneInfo> getGenes(String prefix) throws ParseException, IOException {
        prefix = URLDecoder.decode(prefix, UTF_8);
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

        final List<String> geneIds = genes.stream().map(g -> g.getEntrezId().toString()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(geneIds)) {
            final List<GeneId> ensemblIds = ncbiEnsemblIdsManager.searchByEntrezIds(geneIds);
            final Map<Long, GeneId> ensemblIdsMap = ensemblIds.stream()
                    .collect(Collectors.toMap(GeneId::getEntrezId, Function.identity()));
            genes.forEach(g -> {
                if (ensemblIdsMap.containsKey(g.getEntrezId())) {
                    g.setEnsemblId(ensemblIdsMap.get(g.getEntrezId()).getEnsemblId());
                }
            });
        }
        return genes;
    }

    public long getStructuresCount(final Long targetId, final List<String> geneIds) throws ParseException, IOException {
        long localPdbFiles = pdbFileManager.totalCount(geneIds);
        final List<String> geneNames = getGeneNames(targetId, geneIds);
        long structuresCount = pdbEntriesManager.getStructuresCount(geneNames);
        return localPdbFiles + structuresCount;
    }

    public long getPublicationsCount(final Long targetId, final List<String> geneIds, final List<GeneId> ncbiGeneIds)
            throws ParseException, IOException {
        if (targetId != null) {
            final Target target = targetManager.getTarget(targetId);
            if (TargetType.PARASITE.equals(target.getType())) {
                return pubMedService.getParasitesPublicationsCount(getPublicationsQuery(targetId, geneIds));
            }
        }
        final List<String> entrezGeneIds = ncbiGeneIds.stream()
                .map(g -> g.getEntrezId().toString())
                .collect(Collectors.toList());
        return CollectionUtils.isNotEmpty(entrezGeneIds) ? pubMedService.getPublicationsCount(entrezGeneIds) : 0;
    }

    public DrugsCount getDrugsCount(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DGIDBDrugAssociation> dgidbDrugs = dgidbDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DrugAssociation> drugAssociations = drugAssociationManager.searchByGeneIds(geneIds);
        final List<TTDDrugAssociation> ttdDrugs = getTTDDrugs(targetId, geneIds);

        final List<String> pharmGKBDrugNames = pharmGKBDrugs.stream().map(UrlEntity::getName)
                .collect(Collectors.toList());
        final List<String> dgidbDrugsNames = dgidbDrugs.stream().map(UrlEntity::getName).collect(Collectors.toList());
        final List<String> drugNames = drugAssociations.stream().map(UrlEntity::getName).collect(Collectors.toList());
        drugNames.addAll(pharmGKBDrugNames);
        drugNames.addAll(dgidbDrugsNames);
        drugNames.addAll(ttdDrugs.stream().map(UrlEntity::getName).collect(Collectors.toList()));
        final long distinctCount = drugNames.stream().map(String::toLowerCase).distinct().count();

        final long pharmGKBDrugsCount = pharmGKBDrugs.size();
        final long dgidbDrugsCount = dgidbDrugs.size();
        final long openTargetDrugs = drugAssociations.size();
        final long ttdDrugsCount = ttdDrugs.size();

        return DrugsCount.builder()
                .distinctCount(distinctCount)
                .totalCount(pharmGKBDrugsCount + dgidbDrugsCount + openTargetDrugs + ttdDrugsCount)
                .build();
    }

    public List<String> getDrugs(final Long targetId, final List<String> geneIds) throws IOException, ParseException {
        targetManager.expandTargetGenes(targetId, geneIds);
        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DGIDBDrugAssociation> dgidbDrugs = dgidbDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DrugAssociation> drugAssociations = drugAssociationManager.searchByGeneIds(geneIds);
        final List<TTDDrugAssociation> ttdDrugs = getTTDDrugs(targetId, geneIds);

        final List<String> pharmGKBDrugNames = pharmGKBDrugs.stream().map(UrlEntity::getName)
                .collect(Collectors.toList());
        final List<String> dgidbDrugsNames = dgidbDrugs.stream().map(UrlEntity::getName).collect(Collectors.toList());
        final List<String> drugNames = drugAssociations.stream().map(UrlEntity::getName).collect(Collectors.toList());
        final List<String> tddDrugNames = ttdDrugs.stream().map(UrlEntity::getName).collect(Collectors.toList());
        drugNames.addAll(pharmGKBDrugNames);
        drugNames.addAll(dgidbDrugsNames);
        drugNames.addAll(tddDrugNames);
        return drugNames.stream().map(String::toLowerCase).distinct().sorted().collect(Collectors.toList());
    }

    public long getDiseasesCount(final Long targetId, final List<String> geneIds) throws ParseException, IOException {
        final long openTargetsDiseasesCount = diseaseAssociationManager.totalCount(geneIds);
        final long pharmGKBDiseasesCount = pharmGKBDiseaseAssociationManager.totalCount(geneIds);
        final long ttdCount = getTTDDiseases(targetId, geneIds).size();
        return openTargetsDiseasesCount + pharmGKBDiseasesCount + ttdCount;
    }

    public List<String> getGeneNames(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final Set<String> geneNames = new HashSet<>();
        for (String g : geneIds) {
            List<String> targetGeneNames = targetManager.getTargetGeneNames(targetId, Collections.singletonList(g));
            if (CollectionUtils.isEmpty(targetGeneNames)) {
                List<TargetDetails> targetDetails = Collections.emptyList();
                try {
                    targetDetails = targetDetailsManager.search(Collections.singletonList(g));
                } catch (ParseException | IOException e) {
                    log.debug("No gene names found for {}", g);
                }
                targetGeneNames = targetDetails.stream().map(TargetDetails::getSymbol).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(targetGeneNames)) {
                geneNames.addAll(targetGeneNames);
            }
        }
        return new ArrayList<>(geneNames);
    }

    public Map<String, String> getGeneNamesMap(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final Map<String, String> geneNames = new HashMap<>();
        for (String g : geneIds) {
            List<String> targetGeneNames = targetManager.getTargetGeneNames(targetId, Collections.singletonList(g));
            if (CollectionUtils.isEmpty(targetGeneNames)) {
                List<TargetDetails> targetDetails = Collections.emptyList();
                try {
                    targetDetails = targetDetailsManager.search(Collections.singletonList(g));
                } catch (ParseException | IOException e) {
                    log.debug("No gene names found for {}", g);
                }
                targetGeneNames = targetDetails.stream().map(TargetDetails::getSymbol).collect(Collectors.toList());
            }
            if (CollectionUtils.isNotEmpty(targetGeneNames)) {
                geneNames.put(g.toLowerCase(), targetGeneNames.get(0));
            }
        }
        return geneNames;
    }

    public void importTTDData(final String drugsPath, final String targetsPath, final String diseasesPath)
            throws IOException, ParseException {
        ttdDatabaseManager.importData(drugsPath, targetsPath, diseasesPath);
    }

    public SearchResult<TTDDrugAssociation> getTTDDrugs(final AssociationSearchRequest request)
            throws ParseException, IOException {
        final List<TargetGene> genes = targetManager.getTargetGenes(request.getTargetId(), request.getGeneIds());
        return ttdDatabaseManager.getTTDDrugs(genes, request);
    }

    public List<TTDDrugAssociation> getTTDDrugs(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final List<TargetGene> genes = targetManager.getTargetGenes(targetId, geneIds);
        return ttdDatabaseManager.fetchTTDDrugs(genes, Collections.emptyList());
    }

    public TTDDrugFieldValues getTTDDrugFieldValues(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final List<TargetGene> genes = targetManager.getTargetGenes(targetId, geneIds);
        return ttdDatabaseManager.getDrugFieldValues(genes);
    }

    public SearchResult<TTDDiseaseAssociation> getTTDDiseases(final AssociationSearchRequest request)
            throws ParseException, IOException {
        final List<TargetGene> genes = targetManager.getTargetGenes(request.getTargetId(), request.getGeneIds());
        return ttdDatabaseManager.getTTDDiseases(genes, request);
    }

    public List<TTDDiseaseAssociation> getTTDDiseases(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final List<TargetGene> genes = targetManager.getTargetGenes(targetId, geneIds);
        return ttdDatabaseManager.fetchTTDDiseases(genes, Collections.emptyList());
    }

    public TTDDiseaseFieldValues getTTDDiseaseFieldValues(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        final List<TargetGene> genes = targetManager.getTargetGenes(targetId, geneIds);
        return ttdDatabaseManager.getDiseaseFieldValues(genes);
    }

    private String getPublicationsQuery(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final List<String> terms = new ArrayList<>();
        final List<TargetGene> targetGenes = targetManager.getTargetGenes(targetId, geneIds);
        final Set<Long> taxIds = new HashSet<>();
        final Map<String, Set<Long>> geneNamesTaxIdsMap = new HashMap<>();
        targetGenes.forEach(g -> {
            Set<Long> geneTaxIds = new HashSet<>(g.getAdditionalGenes().values());
            geneTaxIds.add(g.getTaxId());
            geneNamesTaxIdsMap.put(g.getGeneName(), geneTaxIds);
            taxIds.addAll(g.getAdditionalGenes().values());
            taxIds.add(g.getTaxId());
        });
        final List<Taxonomy> organisms = taxonomyManager.searchOrganismsByIds(taxIds);
        final Map<Long, Taxonomy> organismsMap = organisms.stream()
                .collect(Collectors.toMap(Taxonomy::getTaxId, Function.identity()));
        geneNamesTaxIdsMap.forEach((k, v) -> {
            v.forEach(t -> {
                if (organismsMap.containsKey(t)) {
                    terms.add(String.format("(%s %s)", k, organismsMap.get(t).getScientificName()));
                }
            });
        });
        return String.join(" OR ", terms);
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
                .url(association.getUrl())
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
}
