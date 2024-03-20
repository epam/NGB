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

import com.epam.catgenome.entity.externaldb.ncbi.GeneId;
import com.epam.catgenome.entity.externaldb.target.DrugsCount;
import com.epam.catgenome.entity.externaldb.target.UrlEntity;
import com.epam.catgenome.entity.externaldb.target.dgidb.DGIDBDrugAssociation;
import com.epam.catgenome.entity.externaldb.target.opentargets.DrugAssociation;
import com.epam.catgenome.entity.externaldb.target.pharmgkb.PharmGKBDrug;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDiseaseAssociation;
import com.epam.catgenome.entity.externaldb.target.ttd.TTDDrugAssociation;
import com.epam.catgenome.entity.target.SequencesSummary;
import com.epam.catgenome.entity.target.TargetGene;
import com.epam.catgenome.entity.target.export.GeneSequenceField;
import com.epam.catgenome.entity.target.export.TargetHomologue;
import com.epam.catgenome.entity.target.export.TargetHomologyField;
import com.epam.catgenome.entity.target.export.xls.TargetExportSummary;
import com.epam.catgenome.entity.target.export.xls.TargetExportSummaryField;
import com.epam.catgenome.exception.ExternalDbUnavailableException;
import com.epam.catgenome.manager.export.ExcelExportUtils;
import com.epam.catgenome.manager.externaldb.PubMedService;
import com.epam.catgenome.manager.externaldb.ncbi.NCBIGeneIdsManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbEntriesManager;
import com.epam.catgenome.manager.externaldb.pdb.PdbStructureField;
import com.epam.catgenome.manager.externaldb.sequence.NCBISequenceManager;
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
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDiseaseField;
import com.epam.catgenome.manager.externaldb.target.ttd.TTDDrugField;
import com.epam.catgenome.manager.pdb.PdbFileField;
import com.epam.catgenome.manager.pdb.PdbFileManager;
import com.epam.catgenome.manager.target.LaunchIdentificationManager;
import com.epam.catgenome.manager.target.TargetManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.epam.catgenome.manager.export.ExcelExportUtils.writeSheet;
import static com.epam.catgenome.manager.target.LaunchIdentificationManager.getGeneIds;
import static com.epam.catgenome.manager.target.export.TargetExportManager.getAssociationFields;


@Service
@RequiredArgsConstructor
public class TargetExportXLSManager {

    private final PharmGKBDrugAssociationManager pharmGKBDrugAssociationManager;
    private final PharmGKBDiseaseAssociationManager pharmGKBDiseaseAssociationManager;
    private final DGIDBDrugAssociationManager dgidbDrugAssociationManager;
    private final DrugAssociationManager drugAssociationManager;
    private final DiseaseAssociationManager diseaseAssociationManager;
    private final PdbEntriesManager pdbEntriesManager;
    private final PdbFileManager pdbFileManager;
    private final NCBIGeneIdsManager ncbiGeneIdsManager;
    private final PubMedService pubMedService;
    private final NCBISequenceManager geneSequencesManager;
    private final TargetExportManager targetExportManager;
    private final LaunchIdentificationManager launchIdentificationManager;
    private final TargetManager targetManager;

    public InputStream report(final Long targetId,
                              final List<String> genesOfInterest,
                              final List<String> translationalGenes)
            throws IOException, ParseException, ExternalDbUnavailableException {

        final List<String> geneIds = getGeneIds(genesOfInterest, translationalGenes);

        targetManager.expandTargetGenes(targetId, geneIds);
        final Map<String, TargetGene> genesMap = targetExportManager.getTargetGenesMap(targetId, geneIds);
        final Map<String, String> targetNames = targetExportManager.getTargetGeneNames(genesMap);
        final List<TargetHomologue> homologyData = targetExportManager.getHomologyData(targetId, geneIds,
                translationalGenes, targetNames);
        try (Workbook workbook = new XSSFWorkbook()) {
            writeSheet("Summary", CollectionUtils.isNotEmpty(translationalGenes) ?
                            Arrays.asList(TargetExportSummaryField.values()) :
                            Arrays.stream(TargetExportSummaryField.values())
                                    .filter(TargetExportSummaryField::isGene)
                                    .collect(Collectors.toList()),
                    getSummary(targetId, genesOfInterest, translationalGenes, genesMap, homologyData), workbook);
            writeSheet("Associated Diseases(Open Targets)", getAssociationFields(DiseaseField.values()),
                    targetExportManager.getDiseaseAssociations(geneIds, targetNames), workbook);
            writeSheet("Known Drugs(Open Targets)", getAssociationFields(DrugField.values()),
                    targetExportManager.getDrugAssociations(geneIds, targetNames), workbook);
            writeSheet("Associated Diseases(PharmGKB)", getAssociationFields(PharmGKBDiseaseField.values()),
                    targetExportManager.getPharmGKBDiseases(geneIds, targetNames), workbook);
            writeSheet("Known Drugs(PharmGKB)", getAssociationFields(PharmGKBDrugField.values()),
                    targetExportManager.getPharmGKBDrugs(geneIds, targetNames), workbook);
            writeSheet("Associated Diseases(TTD)", getAssociationFields(TTDDiseaseField.values()),
                    targetExportManager.getTTDDiseases(targetId, geneIds, targetNames), workbook);
            writeSheet("Known Drugs(TTD)", getAssociationFields(TTDDrugField.values()),
                    targetExportManager.getTTDDrugs(targetId, geneIds, targetNames), workbook);
            writeSheet("Known Drugs(DGIdb)", getAssociationFields(DGIDBField.values()),
                    targetExportManager.getDGIDBDrugs(geneIds, targetNames), workbook);
            writeSheet("Structures (PDB)", Arrays.asList(PdbStructureField.values()),
                    targetExportManager.getStructures(targetId, geneIds), workbook);
            writeSheet("Structures (Local)", Arrays.asList(PdbFileField.values()),
                    targetExportManager.getPdbFiles(geneIds), workbook);
            writeSheet("Sequences", Arrays.asList(GeneSequenceField.values()),
                    targetExportManager.getSequenceTable(targetId,
                            getGeneIds(genesOfInterest, translationalGenes), targetNames), workbook);
            writeSheet("Homology", Arrays.asList(TargetHomologyField.values()), homologyData, workbook);
            return ExcelExportUtils.export(workbook);
        }
    }

    public InputStream report(final String geneId) throws IOException, ParseException, ExternalDbUnavailableException {
        return report(null, Collections.singletonList(geneId), Collections.emptyList());
    }

    private List<TargetExportSummary> getSummary(final Long targetId,
                                                 final List<String> genesOfInterest,
                                                 final List<String> translationalGenes,
                                                 final Map<String, TargetGene> genesMap,
                                                 final List<TargetHomologue> homologyData)
            throws ParseException, IOException, ExternalDbUnavailableException {
        final List<String> geneIds = getGeneIds(genesOfInterest, translationalGenes);
        targetManager.expandTargetGenes(targetId, geneIds);
        final List<GeneId> ncbiGeneIds = ncbiGeneIdsManager.getNcbiGeneIds(geneIds);
        final Map<String, GeneId> genes = ncbiGeneIds.stream()
                .collect(Collectors.toMap(g -> g.getEnsemblId().toLowerCase(), Function.identity()));
        final Map<String, String> descriptions = launchIdentificationManager.getDescriptions(ncbiGeneIds);

        final Map<String, DrugsCount> drugsCountMap = getDrugsCount(targetId, geneIds);

        final Map<String, Long> pharmGKBDiseases = pharmGKBDiseaseAssociationManager.totalCountMap(geneIds);
        final Map<String, Long> diseases = diseaseAssociationManager.totalCountMap(geneIds);
        final List<TTDDiseaseAssociation> ttdDiseases = targetExportManager.getTTDDiseases(targetId,
                geneIds, Collections.emptyMap());
        final Map<String, List<TTDDiseaseAssociation>> ttdDiseasesMap = ttdDiseases.stream()
                .collect(Collectors.groupingBy(d -> d.getGeneId().toLowerCase()));

        final Map<String, SequencesSummary> sequencesSummaryMap =
                targetExportManager.getGeneSequencesCount(targetId, geneIds, ncbiGeneIds);

        final Map<String, Long> structuresCount = getStructuresCount(genesMap);

        final List<TargetExportSummary> summaries = new ArrayList<>();
        for (String geneId : geneIds) {
            boolean isGeneOfInterest = genesOfInterest.stream()
                    .map(String::toLowerCase)
                    .anyMatch(g -> g.equals(geneId));

            long publicationsCount = genes.containsKey(geneId) ? pubMedService.getPublicationsCount(
                    Collections.singletonList(genes.get(geneId).getEntrezId().toString())) : 0;

            Long homologs = isGeneOfInterest ?
                    homologyData.stream().filter(g -> g.getGeneId().equals(geneId)).count() : null;

            long diseasesCount = (pharmGKBDiseases.containsKey(geneId) ? pharmGKBDiseases.get(geneId) : 0) +
                    (diseases.containsKey(geneId) ? diseases.get(geneId) : 0) +
                    (ttdDiseasesMap.containsKey(geneId) ? ttdDiseasesMap.get(geneId).size() : 0);

            String sequences = sequencesSummaryMap.containsKey(geneId) ?
                    sequencesSummaryMap.get(geneId).toString() : "";

            long localPdbFilesCount = pdbFileManager.totalCount(Collections.singletonList(geneId));
            String geneName = genesMap.containsKey(geneId) ? genesMap.get(geneId).getGeneName() : "";
            long structures = (structuresCount.containsKey(geneName) ? structuresCount.get(geneName) : 0) +
                    localPdbFilesCount;

            DrugsCount drugsCount = drugsCountMap.containsKey(geneId) ?
                    drugsCountMap.get(geneId) : DrugsCount.builder().build();

            TargetExportSummary summary = TargetExportSummary.builder()
                    .gene(geneName)
                    .geneId(geneId)
                    .species(genesMap.containsKey(geneId) ? genesMap.get(geneId).getSpeciesName() : "")
                    .description(descriptions.get(geneId))
                    .knownDrugs(drugsCount.getDistinctCount())
                    .knownDrugRecords(drugsCount.getTotalCount())
                    .diseases(diseasesCount)
                    .publications(publicationsCount)
                    .sequences(sequences)
                    .structures(structures)
                    .homologs(homologs)
                    .build();
            if (CollectionUtils.isNotEmpty(translationalGenes)) {
                summary.setType(isGeneOfInterest ? "Gene of interest" : "Translational gene");
            }
            summaries.add(summary);
        }
        return summaries;
    }

    private Map<String, DrugsCount> getDrugsCount(final Long targetId, final List<String> geneIds)
            throws IOException, ParseException {
        final Map<String, DrugsCount> result = new HashMap<>();

        final List<PharmGKBDrug> pharmGKBDrugs = pharmGKBDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DGIDBDrugAssociation> dgidbDrugs = dgidbDrugAssociationManager.searchByGeneIds(geneIds);
        final List<DrugAssociation> drugAssociations = drugAssociationManager.searchByGeneIds(geneIds);
        final List<TTDDrugAssociation> ttdDrugs = targetExportManager.getTTDDrugs(targetId,
                geneIds, Collections.emptyMap());

        final Map<String, List<PharmGKBDrug>> pharmGKBDrugsMap = pharmGKBDrugs.stream()
                .collect(Collectors.groupingBy(d -> d.getGeneId().toLowerCase()));
        final Map<String, List<DGIDBDrugAssociation>> dgidbDrugsMap = dgidbDrugs.stream()
                .collect(Collectors.groupingBy(d -> d.getGeneId().toLowerCase()));
        final Map<String, List<DrugAssociation>> drugAssociationsMap = drugAssociations.stream()
                .collect(Collectors.groupingBy(d -> d.getGeneId().toLowerCase()));
        final Map<String, List<TTDDrugAssociation>> ttdDrugsMap = ttdDrugs.stream()
                .collect(Collectors.groupingBy(d -> d.getGeneId().toLowerCase()));

        for (String geneId : geneIds) {
            List<String> pharmGKBDrugNames = pharmGKBDrugsMap.getOrDefault(geneId, Collections.emptyList())
                    .stream().map(UrlEntity::getName).collect(Collectors.toList());
            List<String> dgidbDrugsNames = dgidbDrugsMap.getOrDefault(geneId, Collections.emptyList())
                    .stream().map(UrlEntity::getName).collect(Collectors.toList());
            List<String> drugNames = drugAssociationsMap.getOrDefault(geneId, Collections.emptyList())
                    .stream().map(UrlEntity::getName).collect(Collectors.toList());
            List<String> ttdDrugNames = ttdDrugsMap.getOrDefault(geneId, Collections.emptyList())
                    .stream().map(UrlEntity::getName).collect(Collectors.toList());
            drugNames.addAll(pharmGKBDrugNames);
            drugNames.addAll(dgidbDrugsNames);
            drugNames.addAll(ttdDrugNames);
            long distinctCount = drugNames.stream().map(String::toLowerCase).distinct().count();
            long totalCount = pharmGKBDrugsMap.getOrDefault(geneId, Collections.emptyList()).size() +
                    dgidbDrugsMap.getOrDefault(geneId, Collections.emptyList()).size() +
                    drugAssociationsMap.getOrDefault(geneId, Collections.emptyList()).size() +
                    ttdDrugsMap.getOrDefault(geneId, Collections.emptyList()).size();
            DrugsCount drugsCount = DrugsCount.builder()
                    .distinctCount(distinctCount)
                    .totalCount(totalCount)
                    .build();
            result.put(geneId, drugsCount);
        }
        return result;
    }

    private Map<String, Long> getStructuresCount(final Map<String, TargetGene> genesMap) {
        final List<String> geneNames = genesMap.values().stream()
                .map(TargetGene::getGeneName)
                .distinct()
                .collect(Collectors.toList());
        final Map<String, Long> structuresCountMap = new HashMap<>();
        geneNames.forEach(geneName -> {
            long structuresCount = pdbEntriesManager.getStructuresCount(Collections.singletonList(geneName));
            structuresCountMap.put(geneName, structuresCount);
        });
        return structuresCountMap;
    }
}
