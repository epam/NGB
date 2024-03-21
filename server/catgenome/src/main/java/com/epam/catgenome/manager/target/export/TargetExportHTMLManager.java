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
package com.epam.catgenome.manager.target.export;

import com.epam.catgenome.controller.JsonMapper;
import com.epam.catgenome.controller.vo.externaldb.NCBISummaryVO;
import com.epam.catgenome.controller.vo.target.PublicationSearchRequest;
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.target.DrugsCount;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.entity.target.GeneSequence;
import com.epam.catgenome.entity.target.SequencesSummary;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.export.html.ComparativeGenomics;
import com.epam.catgenome.entity.target.export.html.DiseaseData;
import com.epam.catgenome.entity.target.export.html.GeneDetails;
import com.epam.catgenome.entity.target.export.html.KnownDrugData;
import com.epam.catgenome.entity.target.export.html.KnownDrugsCount;
import com.epam.catgenome.entity.target.export.html.LinkEntity;
import com.epam.catgenome.entity.target.export.html.Publication;
import com.epam.catgenome.entity.target.export.html.Sequence;
import com.epam.catgenome.entity.target.export.html.SequenceData;
import com.epam.catgenome.entity.target.export.html.SequenceGene;
import com.epam.catgenome.entity.target.export.html.SequencesCount;
import com.epam.catgenome.entity.target.export.html.SourceData;
import com.epam.catgenome.entity.target.export.html.StructureData;
import com.epam.catgenome.entity.target.export.html.TargetExportHTML;
import com.epam.catgenome.entity.target.export.html.Title;
import com.epam.catgenome.entity.target.export.html.TotalCounts;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.SearchResult;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.target.LaunchIdentificationManager;
import com.epam.catgenome.manager.target.TargetManager;
import com.epam.catgenome.entity.target.export.TargetHomologue;
import com.epam.catgenome.util.NgbFileUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.target.LaunchIdentificationManager.*;
import static org.apache.commons.lang3.StringUtils.join;


@Service
public class TargetExportHTMLManager {
    private static final String DATA_PLACEHOLDER = "\"TARGET_IDENTIFICATION_DATA\"";
    private static final Integer BATCH_SIZE = 500;
    private final LaunchIdentificationManager launchIdentificationManager;
    private final TargetExportManager targetExportManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final TargetManager targetManager;
    private final String templatePath;

    public TargetExportHTMLManager(final LaunchIdentificationManager launchIdentificationManager,
                                   final TargetExportManager targetExportManager,
                                   final NCBIGeneIdsManager ncbiGeneIdsManager,
                                   final TargetManager targetManager,
                                   final @Value("${target.export.html.template:}") String templatePath) {
        this.launchIdentificationManager = launchIdentificationManager;
        this.targetExportManager = targetExportManager;
        this.ncbiGeneIdsManager = ncbiGeneIdsManager;
        this.targetManager = targetManager;
        this.templatePath = templatePath;
    }

    public InputStream getHTMLSummary(final List<String> genesOfInterest,
                                      final List<String> translationalGenes,
                                      final Long targetId)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final String template = getTemplate();

        final List<String> geneIds = getGeneIds(genesOfInterest, translationalGenes);
        final Map<String, TargetGene> genesMap = targetExportManager.getTargetGenesMap(targetId, geneIds);

        final List<String> expandedGeneIds = launchIdentificationManager.getExpandedGeneIds(targetId, geneIds, true);
        final Map<String, String> geneNamesMap = targetExportManager.getTargetGeneNames(targetId, expandedGeneIds);
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(expandedGeneIds);

        final List<SourceData<KnownDrugData>> knownDrugs = getKnownDrugs(targetId, expandedGeneIds, geneNamesMap);

        final List<PharmGKBDisease> pharmGKBDiseases = targetExportManager.getPharmGKBDiseases(expandedGeneIds,
                geneNamesMap);
        final List<DiseaseAssociation> diseaseAssociations = targetExportManager.getDiseaseAssociations(expandedGeneIds,
                geneNamesMap);
        final List<TTDDiseaseAssociation> ttdDiseaseAssociations = targetExportManager.getTTDDiseases(targetId,
                geneIds, geneNamesMap);
        final List<SourceData<DiseaseData>> diseases = getDiseases(pharmGKBDiseases,
                diseaseAssociations, ttdDiseaseAssociations);

        final List<GeneRefSection> geneRefSections = launchIdentificationManager.getGeneSequencesTable(targetId,
                geneIds, false, true, true);
        final List<Sequence> sequences = getSequences(targetId, geneIds, geneNamesMap, geneRefSections);
        final List<SourceData<StructureData>> structures = getStructures(targetId, expandedGeneIds);
        final long publicationsCount = launchIdentificationManager.getPublicationsCount(targetId, geneIds, ncbiGeneIds);
        final List<Publication> publications = getPublications(targetId, geneIds, publicationsCount);
        final List<ComparativeGenomics> comparativeGenomics = getComparativeGenomics(targetId, genesOfInterest,
                translationalGenes, geneNamesMap);

        final DrugsCount drugsCount = launchIdentificationManager.getDrugsCount(targetId, expandedGeneIds);
        final long structuresCount = structures.stream().mapToInt(d -> d.getData().size()).sum();
        final long homologsCount = comparativeGenomics.size();
        final SequencesSummary sequencesSummary = getGeneSequencesCount(geneRefSections);
        final KnownDrugsCount knownDrugsCount = KnownDrugsCount
                .builder()
                .drugs(drugsCount.getDistinctCount())
                .records(drugsCount.getTotalCount())
                .build();
        final SequencesCount sequencesCount = SequencesCount
                .builder()
                .dnas(sequencesSummary.getDNAs())
                .mrnas(sequencesSummary.getMRNAs())
                .proteins(sequencesSummary.getProteins())
                .build();
        final TotalCounts totalCounts = TotalCounts.builder()
                .knownDrugs(knownDrugsCount)
                .sequences(sequencesCount)
                .diseases((long) (pharmGKBDiseases.size() + diseaseAssociations.size() + ttdDiseaseAssociations.size()))
                .genomics(homologsCount)
                .structures(structuresCount)
                .publications(publicationsCount)
                .build();

        final List<GeneDetails> interest = new ArrayList<>();
        final List<GeneDetails> translational = new ArrayList<>();
        final Map<String, String> descriptions = launchIdentificationManager.getDescriptions(ncbiGeneIds);
        for (String geneId : geneIds) {
            TargetGene gene = genesMap.containsKey(geneId) ? genesMap.get(geneId) : TargetGene.builder().build();
            boolean isGeneOfInterest = genesOfInterest.stream()
                    .map(String::toLowerCase)
                    .anyMatch(g -> g.equals(geneId));
            GeneDetails details = GeneDetails.builder()
                    .id(geneId)
                    .name(gene.getGeneName())
                    .species(gene.getSpeciesName())
                    .description(descriptions.get(geneId))
                    .build();
            if (isGeneOfInterest) {
                interest.add(details);
            } else {
                translational.add(details);
            }
        }

        final TargetExportHTML result = TargetExportHTML.builder()
                .name(getTargetName(targetId, geneIds))
                .interest(interest)
                .translational(translational)
                .totalCounts(totalCounts)
                .knownDrugs(knownDrugs)
                .associatedDiseases(diseases)
                .sequences(sequences)
                .structures(structures)
                .publications(publications)
                .comparativeGenomics(comparativeGenomics)
                .build();
        return fillTemplate(template, result);
    }

    public InputStream getHTMLSummary(final String geneId)
            throws ParseException, IOException, ExternalDbUnavailableException {
        return getHTMLSummary(Collections.singletonList(geneId), Collections.emptyList(), null);
    }

    private String getTargetName(final Long targetId, final List<String> geneIds) throws ParseException, IOException {
        String name;
        if (targetId != null) {
            final Target target = targetManager.getTarget(targetId);
            name = target.getTargetName();
        } else {
            final List<String> geneNames = launchIdentificationManager.getGeneNames(targetId, geneIds);
            name = geneNames.get(0);
        }
        return name;
    }

    private InputStream fillTemplate(final String template,
                                     final TargetExportHTML result) {
        final String html = template.replace(DATA_PLACEHOLDER,
                String.format("JSON.parse('%s')",
                        JsonMapper.convertDataToJsonStringForQuery(result)
                                .replace("'", "\\'")
                                .replace("\\\"", "\\\\\"")));
        return IOUtils.toInputStream(html);
    }

    private String getTemplate() {
        Assert.isTrue(StringUtils.isNotBlank(templatePath), "HTML report is not configured");
        return NgbFileUtils.readResource(templatePath);

    }

    private SequencesSummary getGeneSequencesCount(final List<GeneRefSection> geneRefSections) {
        return getSequencesSummary(geneRefSections);
    }

    private List<Sequence> getSequences(final Long targetId,
                                        final List<String> geneIds,
                                        final Map<String, String> geneNamesMap,
                                        final List<GeneRefSection> geneRefSections)
            throws ParseException, IOException {
        final Map<String, TargetGene> genesMap = targetExportManager.getTargetGenesMap(targetId, geneIds);
        final List<Sequence> sequences = new ArrayList<>();
        for (GeneRefSection geneRefSection : geneRefSections) {
            SequenceGene sequenceGene = SequenceGene.builder()
                    .id(geneRefSection.getGeneId().toLowerCase())
                    .name(geneNamesMap.get(geneRefSection.getGeneId().toLowerCase()))
                    .build();
            if (genesMap.containsKey(geneRefSection.getGeneId().toLowerCase())) {
                sequenceGene.setSpecies(genesMap.get(geneRefSection.getGeneId().toLowerCase()).getSpeciesName());
            }
            LinkEntity reference = new LinkEntity();
            if (geneRefSection.getReference() != null) {
                reference.setValue(geneRefSection.getReference().getId());
                reference.setLink(geneRefSection.getReference().getUrl());
            }
            List<SequenceData> sequencesData = new ArrayList<>();
            for (GeneSequence geneSequence :
                    Optional.ofNullable(geneRefSection.getSequences()).orElse(Collections.emptyList())) {
                LinkEntity transcript = new LinkEntity();
                if (geneSequence.getMRNA() != null) {
                    transcript.setValue(geneSequence.getMRNA().getId());
                    transcript.setLink(geneSequence.getMRNA().getUrl());
                }
                LinkEntity protein = new LinkEntity();
                if (geneSequence.getProtein() != null) {
                    protein.setValue(geneSequence.getProtein().getId());
                    protein.setLink(geneSequence.getProtein().getUrl());
                }
                SequenceData sequenceData = SequenceData.builder()
                        .target(geneNamesMap.get(geneRefSection.getGeneId().toLowerCase()))
                        .transcript(transcript)
                        .mrnaLength(geneSequence.getMRNA() != null ? geneSequence.getMRNA().getLength() : null)
                        .protein(protein)
                        .proteinName(geneSequence.getProtein() != null ? geneSequence.getProtein().getName() : null)
                        .proteinLength(geneSequence.getProtein() != null ? geneSequence.getProtein().getLength() : null)
                        .build();
                sequencesData.add(sequenceData);
            }
            Sequence sequence = Sequence.builder()
                    .gene(sequenceGene)
                    .reference(reference)
                    .data(sequencesData)
                    .build();
            sequences.add(sequence);
        }
        return sequences;
    }

    private List<Publication> getPublications(final Long targetId,
                                              final List<String> geneIds,
                                              final long publicationsCount)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<NCBISummaryVO> result = new ArrayList<>();
        final int pages = (int) Math.ceil((double) publicationsCount / BATCH_SIZE);
        for (int i = 1; i <= pages; i++) {
            PublicationSearchRequest request = new PublicationSearchRequest();
            request.setTargetId(targetId);
            request.setGeneIds(geneIds);
            request.setPage(i);
            request.setPageSize(BATCH_SIZE);
            SearchResult<NCBISummaryVO> articles = launchIdentificationManager.getPublications(request);
            result.addAll(articles.getItems());
        }
        final List<Publication> publications = new ArrayList<>();
        for (NCBISummaryVO article : result) {
            final Title title = Title.builder()
                    .name(article.getTitle())
                    .link(article.getLink())
                    .build();
            final Publication publication = Publication.builder()
                    .title(title)
                    .authors(article.getAuthors().stream()
                            .map(NCBISummaryVO.NCBIAuthor::getName)
                            .collect(Collectors.toList()))
                    .date(article.getDate())
                    .build();
            publications.add(publication);
        }
        return publications;
    }

    private List<ComparativeGenomics> getComparativeGenomics(final Long targetId,
                                                             final List<String> genesOfInterest,
                                                             final List<String> translationalGenes,
                                                             final Map<String, String> geneNamesMap)
            throws IOException, ParseException {
        final List<TargetHomologue> targetHomologs = targetExportManager.getHomologyData(targetId, genesOfInterest,
                translationalGenes, geneNamesMap);
        final List<ComparativeGenomics> result = new ArrayList<>();
        for (TargetHomologue targetHomologue : targetHomologs) {
            LinkEntity homologue = LinkEntity.builder()
                    .value(targetHomologue.getHomologue())
                    .link(String.format("https://www.ncbi.nlm.nih.gov/gene/%s", targetHomologue.getHomologueId()))
                    .build();
            ComparativeGenomics comparativeGenomics = ComparativeGenomics.builder()
                    .target(targetHomologue.getTarget())
                    .species(targetHomologue.getSpecies())
                    .homologyType(targetHomologue.getHomologyType())
                    .homologue(homologue)
                    .homologyGroup(targetHomologue.getHomologyGroup())
                    .protein(targetHomologue.getProtein())
                    .aa(targetHomologue.getProteinLen())
                    .build();
            result.add(comparativeGenomics);
        }
        return result;
    }

    private List<SourceData<StructureData>> getStructures(final Long targetId, final List<String> geneIds)
            throws ParseException, IOException {
        final List<Structure> pdbStructures = targetExportManager.getStructures(targetId, geneIds);
        final List<PdbFile> pdbFiles = targetExportManager.getPdbFiles(geneIds);

        final List<SourceData<StructureData>> result = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(pdbStructures)) {
            final List<StructureData> pdbStructuresData = new ArrayList<>();
            for (Structure structure : pdbStructures) {
                LinkEntity id = LinkEntity.builder()
                        .value(structure.getId())
                        .link(structure.getUrl())
                        .build();
                StructureData structureData = StructureData.builder()
                        .id(id)
                        .name(structure.getName())
                        .method(structure.getMethod())
                        .source(structure.getSource())
                        .resolution(structure.getResolution())
                        .chains(join(structure.getProteinChains(), "/"))
                        .build();
                pdbStructuresData.add(structureData);
            }
            SourceData<StructureData> pdbSourceData = new SourceData<>();
            pdbSourceData.setSource("PROTEIN_DATA_BANK");
            pdbSourceData.setData(pdbStructuresData);
            result.add(pdbSourceData);
        }
        if (CollectionUtils.isNotEmpty(pdbFiles)) {
            final List<StructureData> localPdbData = new ArrayList<>();
            for (PdbFile pdbFile : pdbFiles) {
                LinkEntity id = LinkEntity.builder()
                        .value(pdbFile.getName())
//                        .link(pdbFile.getPath())
                        .build();
                StructureData structureData = StructureData.builder()
                        .id(id)
                        .name(pdbFile.getPrettyName())
                        .owner(pdbFile.getOwner())
                        .build();
                localPdbData.add(structureData);
            }
            SourceData<StructureData> localPdbSourceData = new SourceData<>();
            localPdbSourceData.setSource("LOCAL_FILES");
            localPdbSourceData.setData(localPdbData);
            result.add(localPdbSourceData);
        }
        return result;
    }

    private List<SourceData<KnownDrugData>> getKnownDrugs(final Long targetId,
                                                          final List<String> geneIds,
                                                          final Map<String, String> geneNames)
            throws ParseException, IOException {

        final List<SourceData<KnownDrugData>> knownDrugsList = new ArrayList<>();

        final List<KnownDrugData> knownDrugsDataList = getKnownDrugsData(geneIds, geneNames);
        if (CollectionUtils.isNotEmpty(knownDrugsDataList)) {
            final SourceData<KnownDrugData> knownDrugs = new SourceData<>();
            knownDrugs.setData(knownDrugsDataList);
            knownDrugs.setSource("OPEN_TARGETS");
            knownDrugsList.add(knownDrugs);
        }

        final List<KnownDrugData> pharmGKBDrugsDataList = getPharmGKBDrugsData(geneIds, geneNames);
        if (CollectionUtils.isNotEmpty(pharmGKBDrugsDataList)) {
            final SourceData<KnownDrugData> pharmGKBDrugs = new SourceData<>();
            pharmGKBDrugs.setData(pharmGKBDrugsDataList);
            pharmGKBDrugs.setSource("PHARMGKB");
            knownDrugsList.add(pharmGKBDrugs);
        }

        final List<KnownDrugData> dgidbDrugsDataList = getDgidbDrugsData(geneIds, geneNames);
        if (CollectionUtils.isNotEmpty(dgidbDrugsDataList)) {
            final SourceData<KnownDrugData> dgidbDrugs = new SourceData<>();
            dgidbDrugs.setData(dgidbDrugsDataList);
            dgidbDrugs.setSource("DGIDB");
            knownDrugsList.add(dgidbDrugs);
        }

        final List<KnownDrugData> ttdDrugsDataList = getTTDDrugsData(targetId, geneIds, geneNames);
        if (CollectionUtils.isNotEmpty(ttdDrugsDataList)) {
            final SourceData<KnownDrugData> ttdDrugs = new SourceData<>();
            ttdDrugs.setData(ttdDrugsDataList);
            ttdDrugs.setSource("TTD");
            knownDrugsList.add(ttdDrugs);
        }

        return knownDrugsList;
    }

    private List<KnownDrugData> getKnownDrugsData(final List<String> geneIds, final Map<String, String> geneNamesMap)
            throws ParseException, IOException {
        final List<DrugAssociation> drugAssociations = targetExportManager.getDrugAssociations(geneIds, geneNamesMap);
        final List<KnownDrugData> knownDrugDataList = new ArrayList<>();
        for (DrugAssociation drugAssociation : drugAssociations) {
            LinkEntity drug = LinkEntity.builder()
                    .value(drugAssociation.getName())
                    .link(drugAssociation.getUrl())
                    .build();
            LinkEntity disease = LinkEntity.builder()
                    .value(drugAssociation.getDisease().getName())
                    .link(drugAssociation.getDisease().getUrl())
                    .build();
            LinkEntity source = LinkEntity.builder()
                    .value(drugAssociation.getSource().getName())
                    .link(drugAssociation.getSource().getUrl())
                    .build();
            KnownDrugData knownDrugData = KnownDrugData.builder()
                    .target(drugAssociation.getTarget())
                    .drug(drug)
                    .type(drugAssociation.getDrugType())
                    .mechanism(drugAssociation.getMechanismOfAction())
                    .action(drugAssociation.getActionType())
                    .disease(disease)
                    .phase(drugAssociation.getPhase())
                    .status(drugAssociation.getStatus())
                    .source(source)
                    .build();
            knownDrugDataList.add(knownDrugData);
        }
        return knownDrugDataList;
    }

    private List<KnownDrugData> getPharmGKBDrugsData(final List<String> geneIds, final Map<String, String> geneNames)
            throws ParseException, IOException {
        final List<PharmGKBDrug> pharmGKBDrugs = targetExportManager.getPharmGKBDrugs(geneIds, geneNames);
        final List<KnownDrugData> knownDrugDataList = new ArrayList<>();
        for (PharmGKBDrug drugAssociation : pharmGKBDrugs) {
            LinkEntity drug = LinkEntity.builder()
                    .value(drugAssociation.getName())
                    .link(drugAssociation.getUrl())
                    .build();
            LinkEntity source = LinkEntity.builder()
                    .value(drugAssociation.getSource())
                    .build();
            KnownDrugData knownDrugData = KnownDrugData.builder()
                    .target(drugAssociation.getTarget())
                    .drug(drug)
                    .source(source)
                    .build();
            knownDrugDataList.add(knownDrugData);
        }
        return knownDrugDataList;
    }

    private List<KnownDrugData> getDgidbDrugsData(final List<String> geneIds, final Map<String, String> geneNames)
            throws ParseException, IOException {
        final List<DGIDBDrugAssociation> dgidbDrugs = targetExportManager.getDGIDBDrugs(geneIds, geneNames);
        final List<KnownDrugData> knownDrugDataList = new ArrayList<>();
        for (DGIDBDrugAssociation drugAssociation : dgidbDrugs) {
            LinkEntity drug = LinkEntity.builder()
                    .value(drugAssociation.getName())
                    .link(drugAssociation.getUrl())
                    .build();
            KnownDrugData knownDrugData = KnownDrugData.builder()
                    .target(drugAssociation.getTarget())
                    .drug(drug)
                    .interactionSource(drugAssociation.getInteractionClaimSource())
                    .interactionType(drugAssociation.getInteractionTypes())
                    .build();
            knownDrugDataList.add(knownDrugData);
        }
        return knownDrugDataList;
    }

    private List<KnownDrugData> getTTDDrugsData(final Long targetId,
                                                final List<String> geneIds,
                                                final Map<String, String> geneNames)
            throws ParseException, IOException {
        final List<TTDDrugAssociation> ttdDrugs = targetExportManager.getTTDDrugs(targetId, geneIds, geneNames);
        final List<KnownDrugData> knownDrugDataList = new ArrayList<>();
        for (TTDDrugAssociation drugAssociation : ttdDrugs) {
            LinkEntity drug = LinkEntity.builder()
                    .value(drugAssociation.getName())
                    .link(drugAssociation.getUrl())
                    .build();
            KnownDrugData knownDrugData = KnownDrugData.builder()
                    .target(drugAssociation.getTarget())
                    .ttdTarget(drugAssociation.getTtdTarget())
                    .drug(drug)
                    .company(drugAssociation.getCompany())
                    .type(drugAssociation.getType())
                    .therapeuticClass(drugAssociation.getTherapeuticClass())
                    .inChI(drugAssociation.getInChI())
                    .inChIKey(drugAssociation.getInChIKey())
                    .canonicalSmiles(drugAssociation.getCanonicalSmiles())
                    .status(drugAssociation.getStatus())
                    .compoundClass(drugAssociation.getCompoundClass())
                    .build();
            knownDrugDataList.add(knownDrugData);
        }
        return knownDrugDataList;
    }

    private List<SourceData<DiseaseData>> getDiseases(final List<PharmGKBDisease> pharmGKBDiseases,
                                                      final List<DiseaseAssociation> diseaseAssociations,
                                                      final List<TTDDiseaseAssociation> ttdDiseaseAssociations) {
        final List<SourceData<DiseaseData>> result = new ArrayList<>();

        final List<DiseaseData> pharmGKBDiseasesList = getPharmGKBDiseasesData(pharmGKBDiseases);
        if (CollectionUtils.isNotEmpty(pharmGKBDiseasesList)) {
            final SourceData<DiseaseData> pharmGKBDiseasesData = new SourceData<>();
            pharmGKBDiseasesData.setSource("PHARMGKB");
            pharmGKBDiseasesData.setData(pharmGKBDiseasesList);
            result.add(pharmGKBDiseasesData);
        }

        final List<DiseaseData> diseasesList = getDiseasesData(diseaseAssociations);
        if (CollectionUtils.isNotEmpty(diseasesList)) {
            final SourceData<DiseaseData> diseasesData = new SourceData<>();
            diseasesData.setSource("OPEN_TARGETS");
            diseasesData.setData(diseasesList);
            result.add(diseasesData);
        }

        final List<DiseaseData> ttdDiseasesList = getTTDDiseasesData(ttdDiseaseAssociations);
        if (CollectionUtils.isNotEmpty(ttdDiseasesList)) {
            final SourceData<DiseaseData> diseasesData = new SourceData<>();
            diseasesData.setSource("TTD");
            diseasesData.setData(ttdDiseasesList);
            result.add(diseasesData);
        }

        return result;
    }

    private List<DiseaseData> getPharmGKBDiseasesData(
            final List<PharmGKBDisease> pharmGKBDiseases) {
        final List<DiseaseData> result = new ArrayList<>();
        for (PharmGKBDisease diseaseAssociation : pharmGKBDiseases) {
            LinkEntity disease = LinkEntity.builder()
                    .value(diseaseAssociation.getName())
                    .link(diseaseAssociation.getUrl())
                    .build();
            DiseaseData diseaseData = DiseaseData.builder()
                    .target(diseaseAssociation.getTarget())
                    .disease(disease)
                    .build();
            result.add(diseaseData);
        }
        return result;
    }

    private List<DiseaseData> getDiseasesData(
            final List<DiseaseAssociation> diseaseAssociations) {
        final List<DiseaseData> result = new ArrayList<>();
        for (DiseaseAssociation diseaseAssociation : diseaseAssociations) {
            LinkEntity disease = LinkEntity.builder()
                    .value(diseaseAssociation.getName())
                    .link(diseaseAssociation.getUrl())
                    .build();
            DiseaseData diseaseData = DiseaseData.builder()
                    .target(diseaseAssociation.getTarget())
                    .disease(disease)
                    .overallScore(diseaseAssociation.getOverallScore())
                    .geneticAssociation(diseaseAssociation.getGeneticAssociationScore())
                    .somaticMutations(diseaseAssociation.getSomaticMutationScore())
                    .drugs(diseaseAssociation.getKnownDrugScore())
                    .pathwaysSystems(diseaseAssociation.getAffectedPathwayScore())
                    .textMining(diseaseAssociation.getLiteratureScore())
                    .animalModels(diseaseAssociation.getAnimalModelScore())
                    .rnaExpression(diseaseAssociation.getRnaExpressionScore())
                    .build();
            result.add(diseaseData);
        }
        return result;
    }

    private List<DiseaseData> getTTDDiseasesData(
            final List<TTDDiseaseAssociation> diseaseAssociations) {
        final List<DiseaseData> result = new ArrayList<>();
        for (TTDDiseaseAssociation diseaseAssociation : diseaseAssociations) {
            LinkEntity disease = LinkEntity.builder()
                    .value(diseaseAssociation.getName())
                    .build();
            DiseaseData diseaseData = DiseaseData.builder()
                    .target(diseaseAssociation.getTarget())
                    .disease(disease)
                    .ttdTarget(diseaseAssociation.getTtdTarget())
                    .clinicalStatus(diseaseAssociation.getClinicalStatus())
                    .build();
            result.add(diseaseData);
        }
        return result;
    }
}
