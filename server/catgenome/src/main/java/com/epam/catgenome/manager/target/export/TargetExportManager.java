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

import com.epam.catgenome.entity.externaldb.homolog.HomologGroup;
import com.epam.catgenome.entity.externaldb.homolog.HomologType;
import com.epam.catgenome.entity.externaldb.homologene.Gene;
import com.epam.catgenome.entity.externaldb.homologene.HomologeneEntry;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDisease;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.target.GeneRefSection;
import com.epam.catgenome.entity.target.GeneSequence;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.export.ExportUtils;
import com.epam.catgenome.manager.export.ExcelExportUtils;
import com.epam.catgenome.manager.externaldb.bindings.rcsbpbd.dto.Structure;
import com.epam.catgenome.manager.externaldb.homolog.HomologManager;
import com.epam.catgenome.manager.externaldb.homologene.HomologeneManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbEntriesManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbStructureField;
import com.epam.catgenome.manager.externaldb.target.AssociationExportField;
import com.epam.catgenome.manager.externaldb.target.AssociationExportFieldDiseaseView;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.dgidb.DGIDBField;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DiseaseField;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.opentargets.DrugField;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDiseaseAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDiseaseField;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugAssociationManager;
import com.epam.catgenome.manager.externaldb.target.pharmgkb.PharmGKBDrugField;
import com.epam.catgenome.manager.target.TargetIdentificationManager;
import com.epam.catgenome.manager.target.TargetManager;
import com.epam.catgenome.util.FileFormat;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.TextUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.catgenome.manager.export.ExcelExportUtils.writeSheet;


@Service
@RequiredArgsConstructor
public class TargetExportManager {

    private static final String TARGET_NAME = "%s (%s)";
    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final PdbEntriesManager pdbEntriesManager;
    private final PdbFileManager pdbFileManager;
    private final TargetManager targetManager;
    private final TargetIdentificationManager identificationManager;
    private final HomologManager homologManager;
    private final HomologeneManager homologeneManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final PubMedService pubMedService;
    private final NCBISequenceManager geneSequencesManager;

    public byte[] export(final String diseaseId, final TargetExportTable source,
                         final FileFormat format, final boolean includeHeader)
            throws ParseException, IOException {
        byte[] result = null;
        switch (source) {
            case OPEN_TARGETS_DISEASES:
                result = ExportUtils.export(diseaseAssociationManager.search(diseaseId),
                        getAssociationFieldsDiseaseView(DiseaseField.values()), format, includeHeader);
                break;
            case OPEN_TARGETS_DRUGS:
                result = ExportUtils.export(drugAssociationManager.search(diseaseId),
                        getAssociationFieldsDiseaseView(DrugField.values()), format, includeHeader);
                break;
            default:
                break;
        }
        return result;
    }

    public byte[] export(final List<String> genesOfInterest,
                         final List<String> translationalGenes,
                         final FileFormat format,
                         final TargetExportTable source,
                         final boolean includeHeader)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final List<String> geneIds = Stream.concat(genesOfInterest.stream(), translationalGenes.stream())
                .distinct().collect(Collectors.toList());
        final Map<String, String> genesMap = getTargetNames(geneIds);
        byte[] result = null;
        switch (source) {
            case OPEN_TARGETS_DISEASES:
                result = ExportUtils.export(getDiseaseAssociations(geneIds, genesMap),
                        getAssociationFields(DiseaseField.values()), format, includeHeader);
                break;
            case OPEN_TARGETS_DRUGS:
                result = ExportUtils.export(getDrugAssociations(geneIds, genesMap),
                        getAssociationFields(DrugField.values()), format, includeHeader);
                break;
            case PHARM_GKB_DISEASES:
                result = ExportUtils.export(getPharmGKBDiseases(geneIds, genesMap),
                        getAssociationFields(PharmGKBDiseaseField.values()), format, includeHeader);
                break;
            case PHARM_GKB_DRUGS:
                result = ExportUtils.export(getPharmGKBDrugs(geneIds, genesMap),
                        getAssociationFields(PharmGKBDrugField.values()), format, includeHeader);
                break;
            case DGIDB_DRUGS:
                result = ExportUtils.export(getDGIDBDrugs(geneIds, genesMap),
                        getAssociationFields(DGIDBField.values()), format, includeHeader);
                break;
            case STRUCTURES:
                result = ExportUtils.export(getStructures(geneIds),
                        Arrays.asList(PdbStructureField.values()), format, includeHeader);
                break;
            case LOCAL_PDBS:
                result = ExportUtils.export(getPdbFiles(geneIds),
                        Arrays.asList(PdbFileField.values()), format, includeHeader);
                break;
            case SEQUENCES:
                result = ExportUtils.export(getSequenceTable(geneIds, genesMap),
                        Arrays.asList(GeneSequenceField.values()), format, includeHeader);
                break;
            case HOMOLOGY:
                result = ExportUtils.export(getHomologyData(genesOfInterest, translationalGenes, genesMap),
                        Arrays.asList(HomologyField.values()), format, includeHeader);
                break;
            default:
                break;
        }
        return result;
    }

    public InputStream report(final List<String> genesOfInterest,
                              final List<String> translationalGenes)
            throws IOException, ParseException, ExternalDbUnavailableException {
        final List<String> geneIds = Stream.concat(genesOfInterest.stream(), translationalGenes.stream())
                .map(String::toLowerCase).distinct().collect(Collectors.toList());
        final Map<String, TargetGene> genesMap = getTargetGenes(geneIds);
        final Map<String, String> targetNames = getTargetNames(genesMap);
        try (Workbook workbook = new XSSFWorkbook()) {
            writeSheet("Summary", Arrays.asList(TargetExportSummaryField.values()),
                    getSummary(genesOfInterest, translationalGenes, genesMap), workbook);
            writeSheet("Associated Diseases(Open Targets)", getAssociationFields(DiseaseField.values()),
                    getDiseaseAssociations(geneIds, genesMap), workbook);
            writeSheet("Known Drugs(Open Targets)", getAssociationFields(DrugField.values()),
                    getDrugAssociations(geneIds, genesMap), workbook);
            writeSheet("Associated Diseases(PharmGKB)", getAssociationFields(PharmGKBDiseaseField.values()),
                    getPharmGKBDiseases(geneIds, genesMap), workbook);
            writeSheet("Known Drugs(PharmGKB)", getAssociationFields(PharmGKBDrugField.values()),
                    getPharmGKBDrugs(geneIds, genesMap), workbook);
            writeSheet("Known Drugs(DGIdb)", getAssociationFields(DGIDBField.values()),
                    getDGIDBDrugs(geneIds, genesMap), workbook);
            writeSheet("Structures (PDB)", Arrays.asList(PdbStructureField.values()),
                    getStructures(geneIds), workbook);
            writeSheet("Structures (Local)", Arrays.asList(PdbFileField.values()),
                    getPdbFiles(geneIds), workbook);
            writeSheet("Sequences", Arrays.asList(GeneSequenceField.values()),
                    getSequenceTable(geneIds, genesMap), workbook);
            writeSheet("Homology", Arrays.asList(HomologyField.values()),
                    getHomologyData(genesOfInterest, translationalGenes, genesMap), workbook);
            return ExcelExportUtils.export(workbook);
        }
    }

    public InputStream report(final String diseaseId) throws IOException, ParseException {
        try (Workbook workbook = new XSSFWorkbook()) {
            writeSheet("Drugs", getAssociationFieldsDiseaseView(DrugField.values()),
                    drugAssociationManager.search(diseaseId), workbook);
            writeSheet("Targets", getAssociationFieldsDiseaseView(DiseaseField.values()),
                    diseaseAssociationManager.search(diseaseId), workbook);
            return ExcelExportUtils.export(workbook);
        }
    }

    private static <T> List<AssociationExportField<T>> getAssociationFields(final AssociationExportField<T>[] array) {
        return Arrays.stream(array).filter(AssociationExportField::isExport).collect(Collectors.toList());
    }

    private static <T> List<AssociationExportFieldDiseaseView<T>> getAssociationFieldsDiseaseView(
            final AssociationExportFieldDiseaseView<T>[] array) {
        return Arrays.stream(array)
                .filter(AssociationExportFieldDiseaseView::isExportDiseaseView)
                .collect(Collectors.toList());
    }

    private List<TargetHomology> getHomologyData(final List<String> genesOfInterest,
                                                 final List<String> translationalGenes,
                                                 final Map<String, String> genesMap)
            throws IOException, ParseException {
        final List<Long> species = targetManager.getTargetGeneSpecies(translationalGenes);
        final Map<String, List<HomologGroup>> homologueGroups = homologManager.searchHomolog(genesOfInterest);
        final Map<String, List<HomologeneEntry>> homologenes = homologeneManager.searchHomologenes(genesOfInterest);
        final List<TargetHomology> homology = new ArrayList<>();
        for (Map.Entry<String, List<HomologGroup>> homologueEntry : homologueGroups.entrySet()) {
            for (HomologGroup group : homologueEntry.getValue()) {
                List<Gene> genes = group.getHomologs().stream()
                        .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                        .collect(Collectors.toList());
                for (Gene gene: genes) {
                    if (!TextUtils.isBlank(gene.getSymbol())) {
                        TargetHomology export = new TargetHomology();
                        export.setGeneId(homologueEntry.getKey());
                        export.setTarget(genesMap.get(homologueEntry.getKey().toLowerCase()));
                        export.setSpecies(gene.getSpeciesScientificName());
                        export.setHomologyType(group.getType().getName());
                        export.setHomologue(gene.getSymbol());
                        export.setHomologyGroup(group.getProteinName());
                        export.setProtein(gene.getTitle());
                        export.setProteinLen(gene.getProtLen());
                        homology.add(export);
                    }
                }
            }
        }
        for (Map.Entry<String, List<HomologeneEntry>> homologenesMapEntry : homologenes.entrySet()) {
            for (HomologeneEntry entry : homologenesMapEntry.getValue()) {
                List<Gene> genes = entry.getGenes().stream()
                        .filter(g -> species.stream().anyMatch(s -> s.equals(g.getTaxId())))
                        .collect(Collectors.toList());
                for (Gene gene: genes) {
                    if (!TextUtils.isBlank(gene.getSymbol())) {
                        TargetHomology export = new TargetHomology();
                        export.setGeneId(homologenesMapEntry.getKey());
                        export.setTarget(genesMap.get(homologenesMapEntry.getKey().toLowerCase()));
                        export.setSpecies(gene.getSpeciesScientificName());
                        export.setHomologyType(HomologType.HOMOLOGUE.getName());
                        export.setHomologue(gene.getSymbol());
                        export.setHomologyGroup(entry.getCaption());
                        export.setProtein(gene.getTitle());
                        export.setProteinLen(gene.getProtLen());
                        homology.add(export);
                    }
                }
            }
        }
        return homology;
    }

    private Map<String, String> getTargetNames(final List<String> geneIds) {
        final Map<String, String> genesMap = new HashMap<>();
        final List<TargetGene> genes = targetManager.getTargetGenes(geneIds);
        for (TargetGene gene: genes) {
            genesMap.put(gene.getGeneId().toLowerCase(),
                    String.format(TARGET_NAME, gene.getGeneName(), gene.getSpeciesName()));
        }
        return genesMap;
    }

    private Map<String, String> getTargetNames(final Map<String, TargetGene> genes) {
        final Map<String, String> genesMap = new HashMap<>();
        genes.forEach((k, v) -> {
            genesMap.put(k, String.format(TARGET_NAME, v.getGeneName(), v.getSpeciesName()));
        });
        return genesMap;
    }

    private Map<String, TargetGene> getTargetGenes(final List<String> geneIds) {
        final Map<String, TargetGene> genesMap = new HashMap<>();
        final List<TargetGene> genes = targetManager.getTargetGenes(geneIds);
        for (TargetGene gene: genes) {
            genesMap.put(gene.getGeneId().toLowerCase(), gene);
        }
        return genesMap;
    }


    private List<DiseaseAssociation> getDiseaseAssociations(final List<String> geneIds,
                                                            final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DiseaseAssociation> diseaseAssociations = diseaseAssociationManager.search(geneIds);
        diseaseAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        return diseaseAssociations;
    }

    private List<DrugAssociation> getDrugAssociations(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DrugAssociation> drugAssociations = drugAssociationManager.search(geneIds);
        drugAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        return drugAssociations;
    }

    private List<PharmGKBDisease> getPharmGKBDiseases(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<PharmGKBDisease> pharmGKBDiseases = pharmGKBDiseaseAssociationManager.search(geneIds);
        pharmGKBDiseases.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        return pharmGKBDiseases;
    }

    private List<PharmGKBDrug> getPharmGKBDrugs(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.search(geneIds);
        pharmGKBDrugs.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        return pharmGKBDrugs;
    }

    private List<DGIDBDrugAssociation> getDGIDBDrugs(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException {
        final List<DGIDBDrugAssociation> dgidbDrugAssociations = dgidbDrugAssociationManager.search(geneIds);
        dgidbDrugAssociations.forEach(d -> d.setTarget(genesMap.get(d.getGeneId().toLowerCase())));
        return dgidbDrugAssociations;
    }

    private List<Structure> getStructures(final List<String> geneIds) {
        final List<String> geneNames = targetManager.getTargetGeneNames(geneIds);
        return pdbEntriesManager.getAllStructures(geneNames);
    }

    private List<PdbFile> getPdbFiles(final List<String> geneIds) {
        return pdbFileManager.load(geneIds);
    }

    private List<GeneSequenceExport> getSequenceTable(final List<String> geneIds, final Map<String, String> genesMap)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<GeneRefSection> sequencesTable = identificationManager.getGeneSequencesTable(geneIds,
                false);
        final List<GeneSequenceExport> result = new ArrayList<>();
        for (GeneRefSection geneRefSection : sequencesTable) {
            for (GeneSequence sequence : geneRefSection.getSequences()){
                GeneSequenceExport sequenceExport = new GeneSequenceExport();
                sequenceExport.setGeneId(geneRefSection.getGeneId());
                sequenceExport.setTarget(genesMap.get(geneRefSection.getGeneId().toLowerCase()));
                if (geneRefSection.getReference() != null) {
                    sequenceExport.setReference(geneRefSection.getReference().getId());
                }
                if (sequence.getMRNA() != null) {
                    sequenceExport.setMRNA(sequence.getMRNA().getId());
                    sequenceExport.setMRNALength(sequence.getMRNA().getLength());
                }
                if (sequence.getProtein() != null) {
                    sequenceExport.setProtein(sequence.getProtein().getId());
                    sequenceExport.setProteinLength(sequence.getProtein().getLength());
                    sequenceExport.setProteinName(sequence.getProtein().getName());
                }
                result.add(sequenceExport);
            }
        }
        return result;
    }

    public List<TargetExportSummary> getSummary(final List<String> genesOfInterest,
                                                final List<String> translationalGenes,
                                                final Map<String, TargetGene> genesMap)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<String> geneIds = Stream.concat(genesOfInterest.stream(), translationalGenes.stream())
                .map(String::toLowerCase).distinct().collect(Collectors.toList());
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        final Map<String, GeneId> genes = ncbiGeneIds.stream()
                .collect(Collectors.toMap(g -> g.getEnsemblId().toLowerCase(), Function.identity()));
        final Map<String, String> descriptions = identificationManager.getDescriptions(ncbiGeneIds);

        final Map<String, Pair<Long, Long>> pharmGKBDrugs = pharmGKBDrugAssociationManager.recordsCountMap(geneIds);
        final Map<String, Pair<Long, Long>> dgidbDrugs = dgidbDrugAssociationManager.recordsCountMap(geneIds);
        final Map<String, Pair<Long, Long>> drugs = drugAssociationManager.recordsCountMap(geneIds);

        final Map<String, Long> pharmGKBDiseases = pharmGKBDiseaseAssociationManager.totalCountMap(geneIds);
        final Map<String, Long> diseases = diseaseAssociationManager.totalCountMap(geneIds);
        final Map<String, SequencesSummary> sequencesSummaryMap =
                geneSequencesManager.getSequencesCountMap(ncbiGeneIds);

        final List<TargetExportSummary> summaries = new ArrayList<>();
        for (String geneId : geneIds) {
            final long publicationsCount = pubMedService.getPublicationsCount(
                Collections.singletonList(genes.get(geneId).getEntrezId().toString()));
            final long structuresCount = pdbEntriesManager.getStructuresCount(
                    Collections.singletonList(genesMap.get(geneId).getGeneName()));

            TargetExportSummary summary = TargetExportSummary.builder()
                    .gene(genesMap.get(geneId).getGeneName())
                    .geneId(genesMap.get(geneId).getGeneId())
                    .species(genesMap.get(geneId).getSpeciesName())
                    .type(genesOfInterest.stream().anyMatch(g -> g.equals(geneId)) ? "Gene of interest" :
                            "Translational gene")
                    .description(descriptions.get(geneId))
                    .knownDrugs(pharmGKBDrugs.get(geneId).getRight() + dgidbDrugs.get(geneId).getRight() +
                            drugs.get(geneId).getRight())
                    .knownDrugRecords(pharmGKBDrugs.get(geneId).getLeft() + dgidbDrugs.get(geneId).getLeft() +
                            drugs.get(geneId).getLeft())
                    .diseases(pharmGKBDiseases.get(geneId) + diseases.get(geneId))
                    .publications(publicationsCount)
                    .sequences(sequencesSummaryMap.get(geneId).toString())
                    .structures(String.valueOf(structuresCount))
                    .build();
            summaries.add(summary);
        }
        return summaries;
    }
}
