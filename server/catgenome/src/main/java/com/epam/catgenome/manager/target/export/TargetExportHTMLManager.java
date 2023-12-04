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
import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.target.DrugsCount;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.pdb.PdbFile;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.entity.target.GeneSequence;
import com.epam.catgenome.entity.target.SequencesSummary;
import com.epam.catgenome.entity.target.Target;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.export.html.*;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.externaldb.PubMedService;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.sequence.NCBISequenceManager;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.apache.commons.lang3.StringUtils.join;


@Service
public class TargetExportHTMLManager {
    private static final String DATA_PLACEHOLDER = "\"TARGET_IDENTIFICATION_DATA\"";
    private final LaunchIdentificationManager launchIdentificationManager;
    private final TargetExportManager targetExportManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final PubMedService pubMedService;
    private final NCBISequenceManager geneSequencesManager;
    private final TargetManager targetManager;
    private final String templatePath;

    public TargetExportHTMLManager(final LaunchIdentificationManager launchIdentificationManager,
                                   final TargetExportManager targetExportManager,
                                   final NCBIGeneIdsManager ncbiGeneIdsManager,
                                   final PubMedService pubMedService,
                                   final NCBISequenceManager geneSequencesManager,
                                   final TargetManager targetManager,
                                   final @Value("${target.export.html.template:}") String templatePath) {
        this.launchIdentificationManager = launchIdentificationManager;
        this.targetExportManager = targetExportManager;
        this.ncbiGeneIdsManager = ncbiGeneIdsManager;
        this.pubMedService = pubMedService;
        this.geneSequencesManager = geneSequencesManager;
        this.targetManager = targetManager;
        this.templatePath = templatePath;
    }

    public InputStream getHTMLSummary(final List<String> genesOfInterest,
                                      final List<String> translationalGenes,
                                      final long targetId)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final String template = getTemplate();

        final Target target = targetManager.getTarget(targetId);

        final List<String> geneIds = Stream.concat(genesOfInterest.stream(), translationalGenes.stream())
                .map(String::toLowerCase).distinct().collect(Collectors.toList());
        final Map<String, String> geneNamesMap = targetExportManager.getTargetGeneNames(geneIds);
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        final List<String> entrezIds = ncbiGeneIds.stream()
                .map(g -> g.getEntrezId().toString())
                .collect(Collectors.toList());

        final List<SourceData<KnownDrugData>> knownDrugs = getKnownDrugs(geneIds, geneNamesMap);
        final List<PharmGKBDisease> pharmGKBDiseases = targetExportManager.getPharmGKBDiseases(geneIds,
                geneNamesMap);
        final List<DiseaseAssociation> diseaseAssociations = targetExportManager.getDiseaseAssociations(geneIds,
                geneNamesMap);
        final List<SourceData<DiseaseData>> diseases = getDiseases(pharmGKBDiseases, diseaseAssociations);
        final List<Sequence> sequences = getSequences(geneIds, geneNamesMap);
        final List<SourceData<StructureData>> structures = getStructures(geneIds);
        long publicationsCount = pubMedService.getPublicationsCount(entrezIds);
        final List<Publication> publications = getPublications(entrezIds, publicationsCount);
        final List<ComparativeGenomics> comparativeGenomics = getComparativeGenomics(genesOfInterest,
                translationalGenes, geneNamesMap);

        final DrugsCount drugsCount = launchIdentificationManager.getDrugsCount(geneIds);
        final long structuresCount = structures.stream().mapToInt(d -> d.getData().size()).sum();
        final long homologsCount = comparativeGenomics.size();
        final SequencesSummary sequencesSummary = geneSequencesManager.getSequencesCount(ncbiGeneIds);
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
                .diseases((long) (pharmGKBDiseases.size() + diseaseAssociations.size()))
                .genomics(homologsCount)
                .structures(structuresCount)
                .publications(publicationsCount)
                .build();

        final List<GeneDetails> interest = new ArrayList<>();
        final List<GeneDetails> translational = new ArrayList<>();
        final Map<String, TargetGene> genesMap = targetExportManager.getTargetGenesMap(geneIds);
        final Map<String, String> descriptions = launchIdentificationManager.getDescriptions(ncbiGeneIds);
        for (String geneId : geneIds) {
            TargetGene gene = genesMap.get(geneId);
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

        TargetExportHTML result = TargetExportHTML.builder()
                .name(target.getTargetName())
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
        final String template = getTemplate();

        final List<String> geneIds = Collections.singletonList(geneId);
        final Map<String, String> genesMap = targetExportManager.getTargetGeneNames(geneId);
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        final List<String> entrezIds = ncbiGeneIds.stream()
                .map(g -> g.getEntrezId().toString())
                .collect(Collectors.toList());

        final List<SourceData<KnownDrugData>> knownDrugs = getKnownDrugs(geneIds, genesMap);
        final List<PharmGKBDisease> pharmGKBDiseases = targetExportManager.getPharmGKBDiseases(geneIds, genesMap);
        final List<DiseaseAssociation> diseaseAssociations = targetExportManager.getDiseaseAssociations(geneIds,
                genesMap);
        final List<SourceData<DiseaseData>> diseases = getDiseases(pharmGKBDiseases, diseaseAssociations);
        final List<Sequence> sequences = getSequences(geneIds, genesMap);
        final List<SourceData<StructureData>> structures = getStructures(geneIds);
        long publicationsCount = pubMedService.getPublicationsCount(entrezIds);
        final List<Publication> publications = getPublications(entrezIds, publicationsCount);
        final List<ComparativeGenomics> comparativeGenomics = getComparativeGenomics(geneIds,
                Collections.emptyList(), genesMap);
        final long homologsCount = comparativeGenomics.size();


        final DrugsCount drugsCount = launchIdentificationManager.getDrugsCount(geneIds);
        final long structuresCount = structures.stream().mapToInt(d -> d.getData().size()).sum();

        final SequencesSummary sequencesSummary = geneSequencesManager.getSequencesCount(ncbiGeneIds);
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
                .diseases((long) (pharmGKBDiseases.size() + diseaseAssociations.size()))
                .genomics(homologsCount)
                .structures(structuresCount)
                .publications(publicationsCount)
                .build();
        final List<String> geneNames = launchIdentificationManager.getGeneNames(geneIds);
        TargetExportHTML result = TargetExportHTML.builder()
                .name(geneNames.get(0))
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

    private List<Sequence> getSequences(final List<String> geneIds, final Map<String, String> geneNamesMap)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final Map<String, TargetGene> genesMap = targetExportManager.getTargetGenesMap(geneIds);
        final List<GeneRefSection> sequencesTable = launchIdentificationManager.getGeneSequencesTable(geneIds,
                false);
        final List<Sequence> sequences = new ArrayList<>();
        for (GeneRefSection geneRefSection : sequencesTable) {
            SequenceGene sequenceGene = SequenceGene.builder()
                    .id(geneRefSection.getGeneId())
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
            for (GeneSequence geneSequence : geneRefSection.getSequences()) {
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

    private List<Publication> getPublications(final List<String> entrezIds, final long publicationsCount) {
        final List<NCBISummaryVO> articles = pubMedService.fetchPubMedArticles(entrezIds, publicationsCount);
        final List<Publication> publications = new ArrayList<>();
        for (NCBISummaryVO article : articles) {
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

    private List<ComparativeGenomics> getComparativeGenomics(final List<String> genesOfInterest,
                                                             final List<String> translationalGenes,
                                                             final Map<String, String> geneNamesMap)
            throws IOException, ParseException {
        final List<TargetHomologue> targetHomologs = targetExportManager.getHomologyData(genesOfInterest,
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

    private List<SourceData<StructureData>> getStructures(final List<String> geneIds) {

        final List<Structure> pdbStructures = targetExportManager.getStructures(geneIds);
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

    private List<SourceData<KnownDrugData>> getKnownDrugs(final List<String> geneIds,
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

    private List<SourceData<DiseaseData>> getDiseases(final List<PharmGKBDisease> pharmGKBDiseases,
                                                      final List<DiseaseAssociation> diseaseAssociations) {
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
}
